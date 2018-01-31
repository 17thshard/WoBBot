package wiresegal.wob

import com.fasterxml.jackson.databind.node.ObjectNode
import de.btobastian.javacord.DiscordApi
import de.btobastian.javacord.DiscordApiBuilder
import de.btobastian.javacord.entities.message.Message
import de.btobastian.javacord.entities.message.embed.EmbedBuilder
import de.btobastian.javacord.exceptions.DiscordException
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import org.jsoup.select.Evaluator.*
import java.awt.Color
import java.net.URLEncoder
import java.util.*

/**
 * @author WireSegal
 * Created at 9:12 PM on 1/25/18.
 */


// https://discordapp.com/oauth2/authorize?client_id=406271036913483796&scope=bot&permissions=8192

val arcanumColor = Color(0x003A52)
const val iconUrl = "https://cdn.discordapp.com/emojis/373082865073913859.png?v=1"
val rattles = arrayOf(
    "You've killed me. Bastards, you've killed me! While the sun is still hot, I die!" to "Collected on Chachabah 1171, 10 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed soldier thirty-one years of age. Sample is considered questionable",
    "The love of men is a frigid thing, a mountain stream only three steps from the ice. We are his. Oh Stormfather... we are his. It is but a thousand days, and the Everstorm comes" to "Collected on Shashahes 1171, 31 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed pregnant woman of middle years. The child did not survive",
    "Ten orders. We were loved, once. Why have you forsaken us, Almighty! Shard of my soul, where have you gone?" to "Collected on Kakan 1171, 5 seconds pre-death, by the Silent Gatherers. Subject was a lighteyed woman in her third decade",
    "A man stood on a cliffside and watched his homeland fall into dust. The waters surged beneath, so far beneath. And he heard a child crying. They were his own tears." to "Collected on Tanatesev 1171, 30 seconds pre-death, by the Silent Gatherers. Subject was a cobbler of some renown.",
    "I'm dying, aren't I? Healer, why do you take my blood? Who is that beside you, with his head of lines? I can see a distant sun, dark and cold, shining in a black sky" to "Collected on Jesanach 1172, 11 seconds pre-death, by the Silent Gatherers. Subject was a Reshi chull trainer. Sample is of particular note.",
    "I have seen the end, and have heard it named. The Night of Sorrows, the True Desolation. The Everstorm." to "Collected on Naneses 1172, 15 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed youth of unknown origin.",
    "I'm cold. Mother, I'm cold. Mother? Why can I still hear the rain? Will it stop?" to "Collected on Vevishes 1172, 32 seconds pre-death, by the Silent Gatherers. Subject was a lighteyed female child, approximately six years old.",
    "They are aflame. They burn. They bring the darkness when they come, and so all you can see is that their skin is aflame. Burn, burn, burn..." to "Collected on Palahishev 1172, 21 seconds pre-death, by the Silent Gatherers. Subject was a baker's apprentice.",
    "Victory! We stand atop the mount! We scatter them before us! Their homes become our dens, their lands are now our farms! And they shall burn, as we once did, in a place that is hollow and forlorn." to "Collected on Ishashan 1172, 18 seconds pre-death, by the Silent Gatherers. Subject was a lighteyed spinster of the eighth dahn.",
    "Ten people, with Shardblades alight, standing before a wall of black and white and red" to "Collected on Jesachev 1173, 12 seconds pre-death, by the Silent Gatherers. Subject an ardent member of the the Silent Gatherers, overheard during his last moments.",
    "Three of sixteen ruled, but now the Broken One reigns" to "Collected on Chachanan 1173, 84 seconds pre-death, by the Silent Gatherers. Subject was a cutpurse with the wasting sickness, of partial Iriali descent.",
    "I'm standing over the body of a brother. I'm weeping. Is that his blood or mine? What have we done?" to "Collected on Vevanev 1173, 107 seconds pre-death, by the Silent Gatherers. Subject was an out-of-work Veden sailor.",
    "He must pick it up, the fallen title! The tower, the crown, and the spear!" to "Collected on Vevahach 1173, 8 seconds pre-death, by the Silent Gatherers. Subject was a prostitute of unknown background.",
    "The burdens of nine become mine. Why must I carry the madness of them all? Oh, Almighty, release me." to "Observed on Palaheses 1173, collected secondhand and later reported to the Silent Gatherers. Subject was a wealthy lighteyes.",
    "A woman sits and scratches out her own eyes. Daughter of kings and winds, the vandal." to "Collected on Palahevan 1173, 73 seconds pre-death, by the Silent Gatherers. Subject was a beggar of some renown, known for his elegant songs.",
    "Light grows so distant. The storm never stops. I am broken, and all around me have died. I weep for the end of all things. He has won. Oh, he has beaten us." to "Collected on Palahakev 1173, 16 seconds pre-death, by the Silent Gatherers. Subject was a Thaylen sailor.",
    "I hold the suckling child in my hands, a knife at his throat, and know that all who live wish me to let the blade slip. Spill its blood upon the ground, over my hands, and with it gain us further breath to draw." to "Collected on Shashanan 1173, 23 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed youth of sixteen years. Sample is of particular note.",
    "Re-Shephir, the Midnight Mother, giving birth to abominations with her essence so dark, so terrible, so consuming. She is here! She watches me die!" to "Collected on Shashabev 1173, 8 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed dock-worker in his forties, father of three.",
    "The death is my life, the strength becomes my weakness, the journey has ended." to "Observed on Betabanes 1173, 95 seconds pre-death, collected secondhand and later reported to the the Silent Gatherers. Subject was a scholar of some minor renown. Sample considered questionable.",
    "Above the final void I hang, friends behind, friends before. The feast I must drink clings to their faces, and the words I must speak spark in my mind. The old oaths will be spoken anew." to "Collected on Betabanan 1173, 45 seconds pre-death, by the Silent Gatherers. Subject was a lighteyed child of five years. Diction improved remarkably when giving sample.",
    "In the storm I awaken, falling, spinning, grieving." to "Collected on Kakanev 1173, 13 seconds pre-death, by the Silent Gatherers. Subject was a city guardsman.",
    "The darkness becomes a palace. Let it rule! Let it rule!" to "Collected on Kakevah 1173, 22 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed Selay man of unknown profession.",
    "I wish to sleep. I know now why you do what you do, and I hate you for it. I will not speak of the truths I see." to "Collected on Kakashah 1173, 142 seconds pre-death, by the Silent Gatherers. Subject was a Shin sailor, left behind by his crew, reportedly for bringing them ill luck. Sample largely useless.",
    "They come from the pit, two dead men, a heart in their hands, and I know that I have seen true glory." to "Collected on Kakashah 1173, 13 seconds pre-death, by the Silent Gatherers. Subject was a rickshaw puller.",
    "I see them. They are the rocks. They are the vengeful spirits. Eyes of red." to "Collected on Kakakes 1173, 8 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed young woman of fifteen who was reportedly mentally unstable since childhood.",
    "That chanting, that singing, those rasping voices." to "Collected on Kakatach 1173, 16 seconds pre-death, by the Silent Gatherers. Subject was a middle-aged potter who reported seeing strange dreams during highstorms during the previous two years.",
    "Let me no longer hurt! Let me no longer weep! Dai-gonarthis! The Black Fisher holds my sorrow and consumes it!" to "Collected on Tanatesach 1173, 28 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed female street juggler.",
    "They named it the Final Desolation, but they lied. Our gods lied. Oh, how they lied. The Everstorm comes. I hear its whispers, see its stormwall, know its heart." to "Collected on Tanatanes 1173, 8 seconds pre-death, by the Silent Gatherers. Subject was an Azish itinerant worker. Sample of particular note.",
    "All is withdrawn for me. I stand against the one who saved my life. I protect the one who killed my promises. I raise my hand. The storm responds." to "Collected on Tanatanev 1173, 18 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed mother of four in her sixty-second year.",
    "Above silence, the illuminating storms—dying storms—illuminate the silence above." to "Collected on Tanatanev 1173, 18 seconds pre-death, by the Silent Gatherers. Subject was an illiterate Herdazian.",
    "He watches! The Black piper in the night. He holds us in his palm... playing a tune that no man can hear!" to "Observed circa 1172. Subject was Cenn, a member of Kaladin's squad in Brightlord Meridas Amaram's army.",
    "They break the land itself! They want it, but in their rage they will destroy it. Like the jealous man burns his rich things rather than let them be taken by his enemies! They come!" to "Observed circa ? 1173. Subject was Gadol, a member of Bridge Four.",
    "And all the world was shattered! The rocks trembled with their steps, and the stones reached toward the heavens. We die! We die!" to "Observed circa ? 1173. Subject was Maps, a member of Bridge Four.",
    "The day was ours, but they took it. Stormfather! You cannot have it. The day is ours. They come, rasping, and the lights fail. Oh, Stormfather!" to "Collected circa Tanat ? 1173 by Taravangian. Subject was a young boy.",
    "So the night will reign, for the choice of honor is life..." to "Observed circa Ishi ? 1173 by Taravangian. Subject was King Valam of Jah Keved.")

val api: DiscordApi = DiscordApiBuilder().setToken(token).login().join()


fun Message.startsWith(string: String) = content.startsWith(string, ignoreCase = true)

fun Element.find(vararg evaluators: Evaluator) = allElements.find(*evaluators)

fun Elements.find(vararg evaluators: Evaluator): Elements {
    var nodes: List<Element> = this
    for (evaluator in evaluators)
        nodes = nodes.filter { it.`is`(evaluator) }
    return Elements(nodes)
}

fun embedFromContent(title: String, url: String, article: Element): EmbedBuilder {
    val embed = EmbedBuilder().setColor(arcanumColor).setTitle(title).setUrl(url).setThumbnail(iconUrl)

    var pending = false

    val lines = mutableListOf<String>()
    var lastSpeaker = "Brandon Sanderson" // Assumed

    val content = article.find(Tag("div"), Class("entry-content")).first()
    val footnote = article.find(Tag("small"), Class("footnote")).first()
    if (footnote != null)
        embed.setFooter(footnote.text())

    val fields = mutableListOf<Pair<String, String>>()

    var lastLine = false

    for (child in content.childNodes()) {
        var text: String? = null
        if (child is Element && child.hasClass("entry-speaker")) {
            if (lines.isNotEmpty()) {
                val str = lines.joinToString("\n").replace("\\s{2,}".toRegex(), " ")
                fields.add(lastSpeaker to str)
                lines.clear()
            }
            lastLine = false
            lastSpeaker = child.text()
            if (lastSpeaker.contains("[PENDING REVIEW]")) {
                pending = true
                lastSpeaker = lastSpeaker.replace("[PENDING REVIEW]", "").trim()
            }
        } else if (child is Element && child.tag().isInline) {
            var line = child.text()

            if (child.tagName() == "p" || lines.isEmpty()) text = line
            else {
                when {
                    child.tagName() == "i" -> line = "_${line}_"
                    child.tagName() == "b" -> line = "**$line**"
                    child.tagName() == "u" -> line = "__${line}__"
                }
                lines[lines.size - 1] = lines.last() + line
                lastLine = true
            }
        } else if (child is Element) text = child.text()
        else if (child is TextNode) text = child.text()

        if (text != null) {
            if (lastLine)
                lines[lines.size - 1] = lines.last() + text
            else
                lines.add(text)
            lastLine = false
        }
    }

    if (pending)
        embed.setDescription("__**Pending Review**__")

    if (lines.isNotEmpty()) {
        val str = lines
                .joinToString("\n") { it.replace("\\s{2,}".toRegex(), " ") }
                .replace("\n{3,}".toRegex(), "\n\n")
        fields.add(lastSpeaker to str)
    }

    var lastJson = embed.toJsonNode()
    for ((author, comment) in fields.filter { it.second.isNotBlank() }
            .map { (author, comment) ->
                if (comment.length > 1024) author to comment.substring(0, 1000)
                        .replace("\\w+$".toRegex(), "").trim() + " *… (Check Arcanum for more.)*"
                else author to comment
            }) {
        embed.addField(author, comment, false)
        val newJson = embed.toJsonNode()
        if (newJson.toString().length > 1950) {
            val footer = lastJson.putObject("footer")
            footer.put("text", "(Too long to display. Check Arcanum for more.)")
            return object : EmbedBuilder() {
                override fun toJsonNode(`object`: ObjectNode): ObjectNode {
                    return `object`.setAll(lastJson) as ObjectNode
                }
            }
        }
        lastJson = newJson
    }

    if (embed.toJsonNode().toString().length > 2000)
        return backupEmbed(title, url)

    return embed
}

fun backupEmbed(title: String, url: String): EmbedBuilder {
    val backup = EmbedBuilder().setColor(arcanumColor).setTitle(title).setUrl(url).setThumbnail(iconUrl)
    backup.setDescription("This WoB is too long. Click on the link above to see it on Arcanum.")
    return backup
}

const val masterUrl = "https://wob.coppermind.net/adv_search/?ordering=rank&query="

fun harvestFromSearch(terms: List<String>): List<EmbedBuilder> {
    val baseUrl = masterUrl + terms.joinToString("+") { URLEncoder.encode(it, "UTF-8") } + "&page="
    val allArticles = mutableListOf<Element>()
    val allEmbeds = mutableListOf<EmbedBuilder>()

    (1..5).all { harvestFromSearchPage(baseUrl, it, allArticles) }

    for ((idx, article) in allArticles.withIndex()) {
        val title = article.find(Tag("header"), Class("entry-options")).first().find(Tag("a")).first()
        val titleText = "Search: \"${terms.joinToString()}\" (${idx+1}/${allArticles.size}) \n" + title.text()
        allEmbeds.add(embedFromContent(titleText, "https://wob.coppermind.net" + title.attr("href"), article))
    }

    return allEmbeds
}

fun harvestFromSearchPage(url: String, page: Int, list: MutableList<Element>): Boolean {
    val data = Jsoup.connect(url + page).get()
    list += data.find(Tag("article"), Class("entry-article"))

    return data.find(Class("fa-chevron-right")).isNotEmpty()
}

const val arrowLeft = "⬅"
const val arrowRight = "➡"
const val done = "\u23F9"
const val last = "⏭"
const val first = "⏮"
const val jumpLeft = "⏪"
const val jumpRight = "⏩"
const val nums = "\uD83D\uDD22"

val validReactions = listOf(arrowLeft, arrowRight, done, last, first, jumpLeft, jumpRight, nums)

val messagesWithEmbedLists = mutableMapOf<Long, Triple<Long, Int, List<EmbedBuilder>>>()

fun updateMessageWithIndex(newIndex: Int, message: Message, entry: Triple<Long, Int, List<EmbedBuilder>>) {
    val (uid, index, embeds) = entry
    if (index != newIndex) {
        val newEmbed = embeds[newIndex]
        message.edit(newEmbed)
        messagesWithEmbedLists[message.id] = Triple(uid, newIndex, embeds)
    }
}

fun updateIndexWithJump(jump: Int, message: Message, entry: Triple<Long, Int, List<EmbedBuilder>>) {
    val (uid, index, embeds) = entry
    val newIndex = Math.min(Math.max(index + jump, 0), embeds.size - 1)
    updateMessageWithIndex(newIndex, message, entry)
}

fun updateIndexToInput(originalMessage: Message, entry: Triple<Long, Int, List<EmbedBuilder>>){
    val (uid, index, embeds) = entry
    var authorReacted = false
    val questionMessage = originalMessage.channel.sendMessage("What number entry would you like to go to?").get()
    while (!authorReacted) {
        //Wait for message equivalent
            val userInput = it.message
            if (userInput.author.id == uid) {
                val numsOnly = userInput.content.replace("[^1-9]".toRegex(), "")
                if (numsOnly != "") {
                    val requestedIndex = numsOnly.toInt() + 1
                    val jump = index - requestedIndex
                    updateIndexWithJump(jump, originalMessage, entry)
                    userInput.delete()
                    questionMessage.delete()
                    authorReacted = true
                }
            }
        }
    }
}

fun search(message: Message, terms: List<String>) {
    val waiting = message.channel.sendMessage("Searching for \"${terms.joinToString().replace("&!", "!")}\"...").get()
    val type = message.channel.typeContinuously()
    val allEmbeds = harvestFromSearch(terms)
    type.close()

    try {
        when {
            allEmbeds.isEmpty() -> message.channel.sendMessage("Couldn't find any WoBs for \"${terms.joinToString().replace("&!", "!")}\".")
            allEmbeds.size == 1 -> {
                val finalEmbed = allEmbeds.first()
                finalEmbed.setTitle(finalEmbed.toJsonNode()["title"].asText().replace(".*\n".toRegex(), ""))
                message.channel.sendMessage("", finalEmbed)
            }
            else -> {
                val search = message.channel.sendMessage("", allEmbeds.first()).get()
                if (allEmbeds.size > 2)
                    search.addReaction(first)
                if (allEmbeds.size > 10)
                    search.addReaction(jumpLeft)
                search.addReaction(arrowLeft)
                search.addReaction(done)
                search.addReaction(arrowRight)
                if (allEmbeds.size > 10)
                    search.addReaction(jumpRight)
                if (allEmbeds.size > 2)
                    search.addReaction(last)
                    search.addReaction(nums)

                messagesWithEmbedLists[search.id] = Triple(message.author.id, 0, allEmbeds)
            }
        }
        waiting.delete()
    } catch (e: DiscordException) {
        message.channel.sendMessage("An error occurred trying to look up the WoB.")
    }
}

fun main(args: Array<String>) {
    api.addMessageCreateListener {
        val message = it.message
        if (!message.userAuthor.orElseGet { api.yourself }.isBot) {
            val content = message.content.toLowerCase(Locale.ROOT)
            val trimmed = content.replace("\\s".toRegex(), "")
            val noChrTrimmed = trimmed.replace("\\W".toRegex(), "")
            if (message.privateChannel.isPresent ||
                    content == "!wob" || content.startsWith("!wob ") || message.mentionedUsers.any { it.isYourself }) {
                if (trimmed == "!wobhelp" || trimmed == api.yourself.mentionTag + "help" ||
                        trimmed == "!wob" || trimmed == api.yourself.mentionTag) {
                    message.channel.sendMessage("Use `!wob \"term\"` to search, or put a WoB link in to get its text directly.")
                } else {
                    val allWobs = "wob\\.coppermind\\.net/events/[\\w-]+/#(e\\d+)".toRegex().findAll(content)

                    for (wob in allWobs) async {
                        val url = "https://" + wob.value
                        val document = Jsoup.connect(url).get()
                        val article = document.find(Tag("article"), Id(wob.groupValues[1])).first()
                        val details = document.find(Tag("div"), Class("eventDetails")).first()
                        if (article != null && details != null) {
                            val title = details.find(ContainsOwnText("Name")).last().parent().text().removePrefix("Name ")
                            val date = details.find(ContainsOwnText("Date")).last().parent().text().removePrefix("Date ")
                            message.channel.sendMessage(embedFromContent("$title ($date)", url, article))
                        }
                    }

                    if (allWobs.none()) {
                        val contentModified = if (message.privateChannel.isPresent && "^[\\w\\s,+!|&]+$".toRegex().matches(content))
                            "\"" + content + "\"" else content

                        val terms = "[\"“]([\\w\\s,+!|&]+)[\"”]".toRegex().findAll(contentModified).toList()
                                .flatMap { it.groupValues[1]
                                        .replace("([^&])!".toRegex(), "$1&!")
                                        .split("[\\s,]+".toRegex())
                                }.filter { it.matches("[!+|&\\w]+".toRegex()) }
                        if (terms.any()) async {
                            search(message, terms)
                        }
                    }
                }
            } else if (noChrTrimmed.startsWith("saythewords"))
                message.channel.sendMessage("**`Life before death.`**\n" +
                        "**`Strength before weakness.`**\n" +
                        "**`Journey before destination.`**")
            else if ("lifebeforedeath" in noChrTrimmed &&
                    "strengthbeforeweakness" in noChrTrimmed &&
                    "journeybeforedestination" in noChrTrimmed)
                message.channel.sendMessage("**`These Words are Accepted.`**")
            else if (noChrTrimmed.startsWith("consultthediagram"))
                message.channel.sendMessage(EmbedBuilder().apply {
                    val rattle = rattles[(Math.random() * rattles.size).toInt()]
                    setColor(Color.RED)
                    setDescription(rattle.first)
                    setFooter(rattle.second)
                })
        }
    }
    api.addReactionAddListener {
        val message = it.message.get()
        val reaction = it.reaction.get()
        if (reaction.emoji.isUnicodeEmoji) {
            val unicode = reaction.emoji.asUnicodeEmoji().get()
            if (!it.user.isBot && unicode in validReactions) {
                val messageValue = messagesWithEmbedLists[message.id]
                if (messageValue != null) {
                    val (uid, index, embeds) = messageValue
                    if (uid == it.user.id) {
                        when (unicode) {
                            arrowLeft -> updateIndexWithJump(-1, message, messageValue)
                            jumpLeft -> updateIndexWithJump(-10, message, messageValue)
                            first -> updateIndexWithJump(-embeds.size, message, messageValue)
                            arrowRight -> updateIndexWithJump(1, message, messageValue)
                            jumpRight -> updateIndexWithJump(10, message, messageValue)
                            last -> updateIndexWithJump(embeds.size, message, messageValue)
                            nums -> updateIndexToInput(message, messageValue)
                            done -> {
                                val finalEmbed = embeds[index]
                                finalEmbed.setTitle(finalEmbed.toJsonNode()["title"].asText().replace(".*\n".toRegex(), ""))
                                message.edit(finalEmbed)
                                messagesWithEmbedLists.remove(message.id)
                                message.removeAllReactions()
                            }
                        }
                    }
                    it.removeReaction()
                }
            }
        }
    }
}
