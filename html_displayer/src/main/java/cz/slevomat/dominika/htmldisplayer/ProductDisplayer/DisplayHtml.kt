package cz.slevomat.dominika.htmldisplayer.ProductDisplayer

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.Log
import cz.slevomat.dominika.htmldisplayer.Models.GroupieItem
import org.jsoup.Jsoup
import kotlin.collections.ArrayList
import kotlinx.coroutines.*

class DisplayHtml{
    private val TAG: String = DisplayHtml::class.java.simpleName
    val groupieItems: ArrayList<GroupieItem> = arrayListOf()
    private var liLevel = 0 //controlling <li> level for setting proper padding when being
    private val TAG_TABLE: String = "table"
    private val TAG_LIST: String = "li"
    private val TAG_LIST_UNORDER: String = "ul"
    private val TAG_LIST_ORDER: String = "ol"
    private val TAG_BREAK: String = "br"
    private val TAG_STRONG: String = "strong"
    private val TAG_BOLD: String = "b"
    private val TAG_EMPH: String = "em"
    private val TAG_ITALIC: String = "i"
    private val TAG_HYPERLINK: String = "a"
    val decoratorArray: ArrayList<String> = arrayListOf()

    /**
     * Parse html using Jsoup
     */
    private fun parseHtml(sHtml: String?): org.jsoup.nodes.Document {
        return Jsoup.parse(sHtml)
    }

    fun createSectionsFromHtml(sHtml: String?, listener: SetDataListener)
            = GlobalScope.launch(Dispatchers.Main, CoroutineStart.DEFAULT) {
        groupieItems.clear()
        try {
            val job = async(Dispatchers.Default) {
                htmlRecursion(
                        parseHtml(sHtml).body().childNodes(), DataType.UNKNOWN, this@DisplayHtml, SpannableStringBuilder(""))
            }
            job.await()
            listener.setDataset(groupieItems)
        } catch (e: Exception) {
            Log.e(TAG, "parsing failed", e)
        }

    }

    /**
     * Recursively go through parsed html and add groupie items to groupieItems array based on their tag
     */
    private fun htmlRecursion(children: MutableList<org.jsoup.nodes.Node>, dataType: DataType, instance : DisplayHtml, spannableString: SpannableStringBuilder): SpannableString {
            if (children.size > 0) {
                var spannableBuilder = SpannableStringBuilder(  "")
                var olCounter = 0
                for (child in children) {
                    if (child.nodeName() == TAG_LIST_UNORDER || child.nodeName() == TAG_LIST_ORDER) liLevel += 1
                    if (child.nodeName() == TAG_BREAK) spannableBuilder.append("\n")
                    if (child.childNodeSize() != 0) {
                        when(child.nodeName()){
                            TAG_LIST_UNORDER -> {
                                if (dataType == DataType.LIST_UNORDERED || dataType == DataType.LIST_ORDERED){
                                    if (liLevel >= 1){
                                        spannableBuilder = Displayer.processElementAndAdd(child, spannableBuilder, liLevel - 1, olCounter, dataType, instance)
                                    }
                                    else spannableBuilder = Displayer.processElementAndAdd(child, spannableBuilder, liLevel, olCounter, dataType, instance)

                                }
                                spannableBuilder.append(htmlRecursion(child.childNodes(),DataType.LIST_UNORDERED, instance, spannableBuilder))
                            }
                            TAG_LIST_ORDER -> spannableBuilder.append(htmlRecursion(child.childNodes(),DataType.LIST_ORDERED, instance, spannableBuilder))

                            TAG_LIST -> spannableBuilder.append(htmlRecursion(child.childNodes(),dataType, instance, spannableBuilder))
                            TAG_TABLE -> spannableBuilder.append(Displayer.processTable(spannableBuilder, child.childNodes(), instance))
                            TAG_ITALIC, TAG_EMPH, TAG_STRONG, TAG_BOLD -> {
                                //because of displaying text which is bold and italic at the same time
                                decoratorArray.add(child.nodeName())
                                spannableBuilder.append(htmlRecursion(child.childNodes(),DataType.UNKNOWN, instance, spannableBuilder))
                                decoratorArray.removeAt(decoratorArray.size - 1)
                            }
                            TAG_HYPERLINK -> {
                                spannableBuilder.append(Displayer.processHyperlink(child, spannableBuilder))
                            }
                            else -> {
                                spannableBuilder.append(htmlRecursion(child.childNodes(),DataType.UNKNOWN, instance, spannableBuilder))
                            }
                        }
                        when(child.nodeName()){
                            TAG_LIST_ORDER, TAG_LIST_UNORDER -> liLevel -= 1
                            TAG_LIST -> {
                                if (dataType == DataType.LIST_ORDERED) olCounter += 1
                            }
                        }
                        spannableBuilder = Displayer.processElementAndAdd(child, spannableBuilder, liLevel, olCounter, dataType, instance)
                    } else {
                        spannableBuilder = SpannableStringBuilder(
                                Displayer.manageAttributes(child.attributes(), child.parentNode().nodeName(),spannableBuilder,instance))
                    }
                }
                return SpannableString(spannableBuilder)
            }
            else return SpannableString("")
        }

    interface SetDataListener {
        fun setDataset(dataset: ArrayList<GroupieItem>)
    }
}