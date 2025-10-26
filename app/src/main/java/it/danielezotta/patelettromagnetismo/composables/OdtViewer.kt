package it.danielezotta.patelettromagnetismo.composables

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OdtViewer(
    context: Context,
    odtFile: File,
    title: String,
    onBackClick: () -> Unit
) {
    var html by remember { mutableStateOf("") }
    var baseDir by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(odtFile.absolutePath) {
        val result = withContext(Dispatchers.IO) { OdtToHtmlConverter.convert(context, odtFile) }
        html = result.html
        baseDir = result.baseDir
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.apply {
                        javaScriptEnabled = false
                        domStorageEnabled = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        allowFileAccess = true
                        allowFileAccessFromFileURLs = true
                    }
                    webViewClient = WebViewClient()
                }
            },
            update = { webView ->
                val base = baseDir?.let { "file://" + it.absolutePath + "/" } ?: ""
                webView.loadDataWithBaseURL(base, html, "text/html", "utf-8", null)
            }
        )
    }
}

private object OdtToHtmlConverter {
    data class Result(val html: String, val baseDir: File?)
    private data class Styles(
        val paragraph: MutableMap<String, MutableMap<String, String>>, // name -> css map
        val text: MutableMap<String, MutableMap<String, String>>, // name -> css map
        val graphic: MutableMap<String, MutableMap<String, String>>, // name -> css map for images/frames
        val parentStyles: MutableMap<String, String>, // style name -> parent name
        val pageCss: MutableMap<String, String>, // page-level CSS (width, height, padding from margins)
        val headerHtml: String,
        val footerHtml: String
    )

    suspend fun convert(context: Context, odtFile: File): Result {
        return withContext(Dispatchers.IO) {
            ZipFile(odtFile).use { zip ->
                val contentEntry: ZipEntry = zip.getEntry("content.xml")
                    ?: return@withContext Result(simpleErrorHtml("Documento non valido"), null)

                val imagesDir = File(context.cacheDir, "odt_images_" + odtFile.nameWithoutExtension)
                if (!imagesDir.exists()) imagesDir.mkdirs()

                // Extract images from Pictures/
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val e = entries.nextElement()
                    if (e.name.startsWith("Pictures/") && !e.isDirectory) {
                        val outFile = File(imagesDir, e.name.removePrefix("Pictures/"))
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(e).use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }

                val styles = parseStyles(zip)
                val contentStream = zip.getInputStream(contentEntry)
                val htmlBody = parseOdtContentXml(contentStream, styles)
                val html = wrapHtml(htmlBody, includeCss = true, pageCss = styles.pageCss)
                Result(html, imagesDir)
            }
        }
    }

    private fun parseStyles(zip: ZipFile): Styles {
        val paragraph = mutableMapOf<String, MutableMap<String, String>>()
        val text = mutableMapOf<String, MutableMap<String, String>>()
        val graphic = mutableMapOf<String, MutableMap<String, String>>()
        val parentStyles = mutableMapOf<String, String>()
        val pageCss = mutableMapOf<String, String>()
        var headerHtml = ""
        var footerHtml = ""

        fun parseStylesFrom(stream: InputStream) {
            val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
            val p = factory.newPullParser()
            p.setInput(stream, "utf-8")
            var event = p.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (p.name) {
                        "page-layout-properties" -> {
                            // Page layout: margins and page size
                            val mt = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-top")
                            val mr = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-right")
                            val mb = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-bottom")
                            val ml = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-left")
                            val pw = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "page-width")
                            val ph = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "page-height")
                            if (pw != null) pageCss["max-width"] = pw
                            if (ph != null) pageCss["min-height"] = ph
                            // Map page margins to container padding
                            val padding = buildString {
                                if (mt != null || mr != null || mb != null || ml != null) {
                                    append((mt ?: "0"))
                                    append(' ')
                                    append((mr ?: "0"))
                                    append(' ')
                                    append((mb ?: "0"))
                                    append(' ')
                                    append((ml ?: "0"))
                                }
                            }
                            if (padding.isNotBlank()) pageCss["padding"] = padding
                        }
                        "header" -> {
                            headerHtml = collectSimpleInlineHtml(p)
                        }
                        
                        "footer" -> {
                            footerHtml = collectSimpleInlineHtml(p)
                        }
                        "style" -> {
                            val family = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "family")
                            val name = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "name")
                            val parent = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "parent-style-name")
                            if (name != null && family != null) {
                                if (parent != null) parentStyles[name] = parent
                                val target = when (family) {
                                    "paragraph" -> paragraph.getOrPut(name) { mutableMapOf() }
                                    "text" -> text.getOrPut(name) { mutableMapOf() }
                                    "graphic" -> graphic.getOrPut(name) { mutableMapOf() }
                                    else -> null
                                }
                                if (target != null) {
                                    var innerEvent = p.next()
                                    while (!(innerEvent == XmlPullParser.END_TAG && p.name == "style")) {
                                        if (innerEvent == XmlPullParser.START_TAG) {
                                            when (p.name) {
                                                "paragraph-properties" -> {
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "text-align")?.let { target["text-align"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-left")?.let { target["margin-left"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-right")?.let { target["margin-right"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-top")?.let { target["margin-top"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-bottom")?.let { target["margin-bottom"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "text-indent")?.let { target["text-indent"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "line-height")?.let { target["line-height"] = it }
                                                }
                                                "graphic-properties" -> {
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "border")?.let { target["border"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "padding")?.let { target["padding"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin")?.let { target["margin"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-left")?.let { target["margin-left"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-right")?.let { target["margin-right"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-top")?.let { target["margin-top"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "margin-bottom")?.let { target["margin-bottom"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "wrap")?.let { wrap ->
                                                        when (wrap) {
                                                            "left" -> target["float"] = "left"
                                                            "right" -> target["float"] = "right"
                                                            "parallel" -> target["float"] = "left" // default to left for parallel
                                                            "none" -> target["display"] = "block"
                                                        }
                                                    }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "horizontal-pos")?.let { hpos ->
                                                        when (hpos) {
                                                            "center" -> {
                                                                target["margin-left"] = "auto"
                                                                target["margin-right"] = "auto"
                                                                target["display"] = "block"
                                                            }
                                                            "left" -> target["float"] = "left"
                                                            "right" -> target["float"] = "right"
                                                        }
                                                    }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "vertical-pos")?.let { /* could map to vertical-align */ }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "background-color")?.let { target["background-color"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "shadow")?.let { target["box-shadow"] = "2px 2px 4px rgba(0,0,0,0.3)" }
                                                }
                                                "text-properties" -> {
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "font-weight")?.let { target["font-weight"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "font-style")?.let { target["font-style"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "color")?.let { target["color"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "font-size")?.let { target["font-size"] = it }
                                                    p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0", "background-color")?.let { target["background-color"] = it }
                                                    val underline = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "text-underline-style")
                                                    if (!underline.isNullOrBlank() && underline != "none") target["text-decoration"] = "underline"
                                                    val lineThrough = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "text-line-through-style")
                                                    if (!lineThrough.isNullOrBlank() && lineThrough != "none") {
                                                        val existing = target["text-decoration"]
                                                        target["text-decoration"] = if (existing != null) "$existing line-through" else "line-through"
                                                    }
                                                    val textPos = p.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:style:1.0", "text-position")
                                                    if (!textPos.isNullOrBlank()) {
                                                        val lower = textPos.lowercase()
                                                        if (lower.contains("super")) {
                                                            target["vertical-align"] = "super"
                                                            if (!target.containsKey("font-size")) target["font-size"] = "0.75em"
                                                        } else if (lower.contains("sub")) {
                                                            target["vertical-align"] = "sub"
                                                            if (!target.containsKey("font-size")) target["font-size"] = "0.75em"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        innerEvent = p.next()
                                    }
                                }
                            }
                        }
                    }
                }
                event = p.next()
            }
        }

        // Parse content.xml automatic-styles
        zip.getEntry("content.xml")?.let { ze -> zip.getInputStream(ze).use { parseStylesFrom(it) } }
        // Parse styles.xml if present
        zip.getEntry("styles.xml")?.let { ze -> zip.getInputStream(ze).use { parseStylesFrom(it) } }
        return Styles(paragraph, text, graphic, parentStyles, pageCss, headerHtml, footerHtml)
    }

    // Collects basic inline HTML content within header/footer blocks (text and line breaks)
    private fun collectSimpleInlineHtml(p: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        var event = p.eventType
        while (true) {
            event = p.next()
            if (event == XmlPullParser.START_TAG) {
                when (p.name) {
                    "p" -> sb.append("<div>")
                    "span" -> sb.append("<span>")
                    "line-break" -> sb.append("<br/>")
                }
                depth++
            } else if (event == XmlPullParser.TEXT) {
                sb.append(escapeHtml(p.text ?: ""))
            } else if (event == XmlPullParser.END_TAG) {
                depth--
                when (p.name) {
                    "p" -> sb.append("</div>")
                    "span" -> sb.append("</span>")
                    "header" -> if (depth == 0) return sb.toString()
                    "footer" -> if (depth == 0) return sb.toString()
                }
                if (depth == 0) return sb.toString()
            }
        }
    }

    private fun parseOdtContentXml(input: InputStream, styles: Styles): String {
        val sb = StringBuilder()
        fun pageOpen(): String {
            val pageStyleInline = if (styles.pageCss.isNotEmpty()) {
                " style=\"" + styles.pageCss.entries.joinToString("; ") { (k, v) -> "$k: $v" } + "\""
            } else ""
            val header = if (styles.headerHtml.isNotBlank()) "<div class=\"header\">${styles.headerHtml}</div>" else ""
            return "<div class=\"page\"$pageStyleInline>$header"
        }
        fun pageClose(): String {
            val footer = if (styles.footerHtml.isNotBlank()) "<div class=\"footer\">${styles.footerHtml}</div>" else ""
            return "$footer</div>"
        }
        var startedPage = false
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser()
        parser.setInput(input, "utf-8")

        var event = parser.eventType
        val openTags = ArrayDeque<String>()

        fun open(tag: String, attrs: String? = null) {
            sb.append('<').append(tag)
            if (!attrs.isNullOrEmpty()) sb.append(' ').append(attrs)
            sb.append('>')
            openTags.addFirst(tag)
        }
        fun close(expected: String) {
            if (openTags.isNotEmpty() && openTags.first() == expected) {
                sb.append("</").append(openTags.removeFirst()).append('>')
            }
        }
        fun text(t: String) { sb.append(escapeHtml(t)) }
        fun getAttr(ns: String?, name: String): String? = try { parser.getAttributeValue(ns, name) } catch (_: Exception) { null }
        fun resolveStyle(name: String?, isParagraph: Boolean): Map<String, String> {
            if (name.isNullOrBlank()) return emptyMap()
            val result = mutableMapOf<String, String>()
            val visited = mutableSetOf<String>()
            var current: String? = name
            while (current != null && !visited.contains(current)) {
                visited.add(current)
                val map = if (isParagraph) styles.paragraph[current] else styles.text[current]
                map?.forEach { (k, v) -> if (!result.containsKey(k)) result[k] = v }
                current = styles.parentStyles[current]
            }
            return result
        }
        fun styleInline(name: String?, isParagraph: Boolean): String? {
            val map = resolveStyle(name, isParagraph)
            if (map.isEmpty()) return null
            val css = map.entries.joinToString("; ") { (k, v) -> "$k: $v" }
            return if (css.isNotBlank()) "style=\"$css\"" else null
        }

        var listDepth = 0
        var listTypeStack = ArrayDeque<String>() // "ul" or "ol"

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name
                    when (name) {
                        "soft-page-break" -> {
                            if (!startedPage) {
                                sb.append(pageOpen())
                                startedPage = true
                            }
                            sb.append(pageClose())
                            sb.append(pageOpen())
                            break
                        }
                        "h" -> {
                            if (!startedPage) { sb.append(pageOpen()); startedPage = true }
                            val levelStr = parser.getAttributeValue(null, "outline-level")
                                ?: parser.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "outline-level")
                            val level = levelStr?.toIntOrNull()?.coerceIn(1, 6) ?: 1
                            val styleName = getAttr("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "style-name")
                                ?: getAttr(null, "style-name")
                            open("h$level", styleInline(styleName, true))
                        }
                        "p" -> {
                            if (!startedPage) { sb.append(pageOpen()); startedPage = true }
                            val styleName = getAttr("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "style-name")
                                ?: getAttr(null, "style-name")
                            open("p", styleInline(styleName, true))
                        }
                        "span" -> {
                            val styleName = getAttr("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "style-name")
                                ?: getAttr(null, "style-name")
                            val attrs = styleInline(styleName, false)
                            open("span", attrs)
                        }
                        "s" -> { // text:s -> spaces
                            val countStr = parser.getAttributeValue(null, "c")
                                ?: parser.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "c")
                            val count = countStr?.toIntOrNull() ?: 1
                            repeat(count) { sb.append("&nbsp;") }
                        }
                        "tab" -> sb.append("&nbsp;&nbsp;&nbsp;&nbsp;") // approximate tab as 4 spaces
                        "line-break" -> sb.append("<br/>")
                        "soft-page-break" -> { /* handled above */ }
                        "image" -> {
                            // Image is inside a frame, we'll handle it when we close the frame
                            // Store href for later use
                        }
                        "a" -> {
                            val href = parser.getAttributeValue("http://www.w3.org/1999/xlink", "href")
                            val safeHref = href?.let { escapeHtml(it) }
                            val attrs = if (safeHref != null) "href=\"$safeHref\" target=\"_blank\" rel=\"noopener noreferrer\"" else null
                            open("a", attrs)
                        }
                        "list" -> {
                            val styleName = getAttr("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "style-name")
                            val listType = "ul" // could be "ol" if we detect numbering
                            listTypeStack.addFirst(listType)
                            listDepth++
                            open(listType)
                        }
                        "list-item" -> open("li")
                        "note" -> {
                            val noteClass = getAttr("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "note-class")
                            sb.append("<sup style=\"color:#0066cc;\">[")
                        }
                        "note-citation" -> { /* text inside will be appended */ }
                        "note-body" -> {
                            sb.append(": ")
                        }
                        "table" -> {
                            val styleName = getAttr("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "style-name")
                            val style = styleInline(styleName, true) ?: "style=\"width:100%; border-collapse: collapse;\""
                            open("table", style)
                        }
                        "table-row" -> open("tr")
                        "table-cell" -> {
                            val styleName = getAttr("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "style-name")
                            val colspan = getAttr("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "number-columns-spanned")
                            val rowspan = getAttr("urn:oasis:names:tc:opendocument:xmlns:table:1.0", "number-rows-spanned")
                            val baseStyle = "border:1px solid #ccc; padding:6px; vertical-align:top;"
                            val inlineStyle = styleInline(styleName, true)
                            val finalStyle = if (inlineStyle != null) {
                                "style=\"$baseStyle ${inlineStyle.removePrefix("style=\"").removeSuffix("\"")}\""
                            } else "style=\"$baseStyle\""
                            val attrs = buildString {
                                append(finalStyle)
                                if (colspan != null && colspan.toIntOrNull() ?: 0 > 1) append(" colspan=\"$colspan\"")
                                if (rowspan != null && rowspan.toIntOrNull() ?: 0 > 1) append(" rowspan=\"$rowspan\"")
                            }
                            open("td", attrs.ifBlank { null })
                        }
                        "frame" -> {
                            // draw:frame contains images or text boxes
                            val styleName = getAttr("urn:oasis:names:tc:opendocument:xmlns:draw:1.0", "style-name")
                            val width = getAttr("urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0", "width")
                            val height = getAttr("urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0", "height")
                            val x = getAttr("urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0", "x")
                            val y = getAttr("urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0", "y")
                            val anchorType = getAttr("urn:oasis:names:tc:opendocument:xmlns:text:1.0", "anchor-type")
                            val zIndex = getAttr("urn:oasis:names:tc:opendocument:xmlns:draw:1.0", "z-index")
                            
                            // Check if this frame contains an image
                            var innerEvent = parser.next()
                            var isImage = false
                            var imageHref: String? = null
                            var imageTitle: String? = null
                            
                            // Peek ahead to see if there's an image inside
                            val startDepth = parser.depth
                            while (innerEvent != XmlPullParser.END_DOCUMENT) {
                                if (innerEvent == XmlPullParser.START_TAG && parser.name == "image") {
                                    isImage = true
                                    imageHref = parser.getAttributeValue("http://www.w3.org/1999/xlink", "href")
                                    imageTitle = parser.getAttributeValue("urn:oasis:names:tc:opendocument:xmlns:draw:1.0", "name")
                                    break
                                }
                                if (innerEvent == XmlPullParser.END_TAG && parser.name == "frame") {
                                    break
                                }
                                innerEvent = parser.next()
                            }
                            
                            if (isImage && imageHref != null) {
                                // Render as img with comprehensive styling
                                val src = if (imageHref.startsWith("Pictures/")) imageHref.removePrefix("Pictures/") else imageHref
                                val styleMap = resolveStyle(styleName, false).toMutableMap()
                                
                                // Add dimension constraints
                                if (width != null) styleMap["width"] = width
                                if (height != null) styleMap["height"] = height
                                
                                // Default responsive behavior if no explicit dimensions
                                if (width == null && height == null) {
                                    styleMap["max-width"] = "100%"
                                    styleMap["height"] = "auto"
                                } else if (width != null && height == null) {
                                    styleMap["height"] = "auto"
                                } else if (width == null && height != null) {
                                    styleMap["width"] = "auto"
                                }
                                
                                // Positioning for absolute anchors
                                if (anchorType == "page" || anchorType == "paragraph") {
                                    if (x != null || y != null) {
                                        styleMap["position"] = "relative"
                                        if (x != null) styleMap["left"] = x
                                        if (y != null) styleMap["top"] = y
                                    }
                                }
                                
                                if (zIndex != null) styleMap["z-index"] = zIndex
                                
                                // Build style string
                                val styleStr = if (styleMap.isNotEmpty()) {
                                    "style=\"" + styleMap.entries.joinToString("; ") { (k, v) -> "$k: $v" } + "\""
                                } else ""
                                
                                val titleAttr = if (!imageTitle.isNullOrBlank()) " alt=\"${escapeHtml(imageTitle)}\" title=\"${escapeHtml(imageTitle)}\"" else ""
                                
                                sb.append("<img src=\"").append(escapeHtml(src)).append("\"").append(titleAttr)
                                if (styleStr.isNotBlank()) sb.append(" ").append(styleStr)
                                sb.append("/>")
                                
                                // Skip to end of frame since we've handled it
                                while (innerEvent != XmlPullParser.END_DOCUMENT) {
                                    if (innerEvent == XmlPullParser.END_TAG && parser.name == "frame") {
                                        break
                                    }
                                    innerEvent = parser.next()
                                }
                            } else {
                                // Text box or other content - render as div
                                val styleMap = resolveStyle(styleName, false).toMutableMap()
                                if (width != null) styleMap["width"] = width
                                if (height != null) styleMap["height"] = height
                                if (x != null) styleMap["left"] = x
                                if (y != null) styleMap["top"] = y
                                if (x != null || y != null) styleMap["position"] = "relative"
                                if (zIndex != null) styleMap["z-index"] = zIndex
                                
                                val styleStr = if (styleMap.isNotEmpty()) {
                                    "style=\"" + styleMap.entries.joinToString("; ") { (k, v) -> "$k: $v" } + "\""
                                } else null
                                
                                open("div", styleStr)
                            }
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val t = parser.text
                    if (!t.isNullOrEmpty()) text(t)
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "h" -> {
                            // close last opened hN
                            val tag = openTags.firstOrNull()
                            if (tag != null && tag.startsWith("h")) close(tag)
                        }
                        "p" -> close("p")
                        "span" -> close("span")
                        "a" -> close("a")
                        "list-item" -> close("li")
                        "list" -> {
                            val listType = listTypeStack.removeFirstOrNull() ?: "ul"
                            close(listType)
                            listDepth--
                        }
                        "note" -> sb.append("]</sup>")
                        "note-citation", "note-body" -> { /* handled inline */ }
                        "table-cell" -> close("td")
                        "table-row" -> close("tr")
                        "table" -> close("table")
                        "frame" -> close("div")
                    }
                }
            }
            event = parser.next()
        }
        if (startedPage) sb.append(pageClose())
        return sb.toString()
    }

    private fun wrapHtml(body: String, includeCss: Boolean, pageCss: Map<String, String> = emptyMap()): String {
        val css = if (includeCss) {
            """
            <style>
                body { font-family: -apple-system, Roboto, Arial, sans-serif; background: #f2f2f2; color: #1b1b1b; }
                .page { max-width: 820px; margin: 12px auto; background:#fff; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
                .header { margin-bottom: 8px; }
                .footer { margin-top: 12px; color: #666; font-size: 0.85em; }
                h1,h2,h3,h4,h5,h6 { margin: 0.8em 0 0.4em; font-weight: 600; }
                h1 { font-size: 1.8em; }
                h2 { font-size: 1.5em; }
                h3 { font-size: 1.3em; }
                h4 { font-size: 1.1em; }
                h5,h6 { font-size: 1em; }
                p { margin: 0.4em 0; line-height: 1.6; }
                img { max-width: 100%; height: auto; display: block; }
                img[style*="float"] { display: inline; }
                ul, ol { margin: 0.5em 0 0.5em 1.5em; padding-left: 0; }
                li { margin: 0.25em 0; }
                table { margin: 0.8em 0; border-collapse: collapse; }
                td, th { font-size: 0.95em; }
                a { color: #0066cc; text-decoration: none; }
                a:hover { text-decoration: underline; }
                strong, b { font-weight: bold; }
                em, i { font-style: italic; }
                sup { font-size: 0.75em; vertical-align: super; }
                sub { font-size: 0.75em; vertical-align: sub; }
            </style>
            """.trimIndent()
        } else ""
        return """
            <html>
            <head>
                <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                $css
            </head>
            <body>$body</body>
            </html>
        """.trimIndent()
    }

    private fun simpleErrorHtml(message: String): String {
        val safe = escapeHtml(message)
        return wrapHtml("<p>" + safe + "</p>", includeCss = true, pageCss = emptyMap())
    }

    private fun escapeHtml(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '&' -> sb.append("&amp;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
