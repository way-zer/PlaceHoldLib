package cf.wayzer.placehold

import org.jetbrains.annotations.TestOnly

object TokenParser {
    @JvmInline
    value class Var(val name: String)

    val PipeToken = Any()

    /**@param tokens list of String|Var|PipeToken*/
    @JvmInline
    value class Expr(val tokens: List<Any>)

    private val tokenEndChar = setOf('|', '}', '{', '"')

    private tailrec fun String.readTokens(parsed0: Int, tokens: MutableList<Any>): Int {
        //skip space
        subSequence(parsed0, length).indexOfFirst { !it.isWhitespace() }
            .takeIf { it > 0 }?.let {
                return readTokens(parsed0 + it, tokens)
            }

        when ((getOrNull(parsed0) ?: return parsed0)) {
            '}' -> return parsed0 //end
            '"' -> { //substring
                if (indexOf('\\', parsed0 + 1) < 0) {//fastPath for no escape
                    val end = indexOf('"', parsed0 + 1)
                    if (end < 0) error("no end for '\"'.(parsed $parsed0)")
                    tokens.add(substring(parsed0 + 1, end))
                    return readTokens(end + 1, tokens)
                }
                val builder = StringBuilder(length - parsed0)
                var end = parsed0 + 1
                while (end < length) {
                    val c = this[end++]
                    when (c) {
                        '"' -> {
                            tokens.add(builder.toString())
                            return readTokens(end, tokens)
                        }

                        '\\' -> if (this[end] == '"') {
                            builder.append('"')
                            end += 1
                            continue
                        }
                    }
                    builder.append(c)
                }
                error("no end for '\"'.(parsed $parsed0)")
            }

            '{' -> error("unexpect char '{'")
            '|' -> {//fastPath
                tokens.add(PipeToken)
                return readTokens(parsed0 + 1, tokens)
            }

            else -> {
                val tokenLen = subSequence(parsed0, length).indexOfFirst { it.isWhitespace() || it in tokenEndChar }
                tokens.add(Var(substring(parsed0, parsed0 + tokenLen)))
                return readTokens(parsed0 + tokenLen, tokens)
            }
        }
    }

    @TestOnly
    fun readTokensTest(text: String, tokens: MutableList<Any>): Int =
        text.readTokens(0, tokens)

    /** @return List<String|Expr> */
    fun parse(text: String): List<Any/*String|Expr*/> {
        if (!text.contains('{')) return listOf(text)//fast path

        return buildList {
            var parsed = 0 //已经处理过的字符
            val textBuilder = StringBuilder()
            while (true) {
                val begin = text.indexOf('{', parsed)
                if (begin == -1) {
                    textBuilder.append(text, parsed, text.length)
                    add(textBuilder.toString())
                    break
                }

                if (parsed < begin)
                    textBuilder.append(text, parsed, begin)
                parsed = begin + 1
                //转义
                if ((text.getOrNull(parsed) ?: break) == '{') {
                    textBuilder.append('{')
                    parsed += 1
                    continue
                }

                if (textBuilder.isNotEmpty()) {
                    add(textBuilder.toString())
                    textBuilder.clear()
                }

                val tokens = buildList {
                    parsed = text.readTokens(parsed, this)
                }
                require(text.getOrNull(parsed) == '}') { "no end for '}'.(parsed $parsed)" }
                parsed++
                //resolve
                add(Expr(tokens))
            }
        }
    }
}