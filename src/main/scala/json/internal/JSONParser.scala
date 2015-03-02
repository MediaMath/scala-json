package json.internal

import scala.collection.immutable.StringOps

trait JSONParser {

  case class JSONBuilderSettings(
      spaceString: String = " ", ignoreNulls: Boolean = true,
      newLineString: String = "\n", tabString: String = "  ") {
    def nTabs(n: Int) = if (tabString == "") ""
    else (for (i <- 0 until n) yield tabString).mkString
  }

  val prettyJSONBuilder = JSONBuilderSettings()
  val denseJSONBuilder = JSONBuilderSettings(
    newLineString = "", tabString = "", spaceString = "")

  //modified some escaping for '/'
  final def quoteJSONString(string: String): StringBuilder = {
    require(string != null)

    val len = string.length
    val sb = new StringBuilder(len + 4)

    sb.append('"')
    for (i <- 0 until len) {
      string.charAt(i) match {
        case c if c == '"' || c == '\\' => //Set('"', '\\') contains c =>
          sb.append('\\')
          sb.append(c)
        //not needed?
        /*case c if c == '/' =>
					//                if (b == '<') {
					sb.append('\\')
					//                }
					sb.append(c)*/
        case '\b' => sb.append("\\b")
        case '\t' => sb.append("\\t")
        case '\n' => sb.append("\\n")
        case '\f' => sb.append("\\f")
        case '\r' => sb.append("\\r")
        case c =>
          if (c < ' ') {
            val t = "000" + Integer.toHexString(c)
            sb.append("\\u" + t.substring(t.length() - 4))
          } else {
            sb.append(c)
          }
      }
    }
    sb.append('"')

    sb
  }

  final def unQuoteJSONString(string: String): String = {
    val sb = new StringBuilder
    val iter = new StringOps(string).iterator

    require(string.charAt(0) == '"', "no starting quote")
    require(string.charAt(string.length - 1) == '"', "no ending quote")

    iter.take(1)

    iter foreach {
      case c if Set('\r', '\n', 0) contains c =>
        sys.error("Unterminated string")
      case '\\' => iter.next match {
        case 'b' =>
          sb.append('\b')
        case 't' =>
          sb.append('\t')
        case 'n' =>
          sb.append('\n')
        case 'f' =>
          sb.append('\f')
        case 'r' =>
          sb.append('\r')
        case 'u' =>
          val n = iter.take(4).mkString
          sb.append(Integer.parseInt(n, 16).toChar)
        case 'x' =>
          val n = iter.take(2).mkString
          sb.append(Integer.parseInt(n, 16).toChar)
        case c =>
          sb.append(c)
      }
      case c if c == '"' =>
      //require(!iter.hasNext, "no ending quote2: " + c)
      case c             => sb.append(c)
    }

    sb.mkString
  }

}