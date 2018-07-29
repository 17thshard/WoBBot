package wiresegal.wob.misc.callouts

import wiresegal.wob.plugin.RegisterHandlers
import wiresegal.wob.plugin.addCalloutHandler
import wiresegal.wob.plugin.gemColorFor
import wiresegal.wob.plugin.sendRandomEmbed
import java.awt.Color

/**
 * Data below is licensed under CC BY-NC-ND.
 * https://creativecommons.org/licenses/by-nc-nd/4.0/legalcode
 *
 * The license is summarized below.
 *
 *** You are free to:
 * Share — copy and redistribute the material in any medium or format
 ** The licensor cannot revoke these freedoms as long as you follow the license terms.
 *** This license is subject to the following terms:
 * Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made.
 *               You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you
 *               or your use.
 * NonCommercial — You may not use the material for commercial purposes.
 * ShareAlike — If you remix, transform, or build upon the material, you must distribute your contributions under
 *              the same license as the original.
 * NoDerivatives — If you remix, transform, or build upon the material, you may not distribute the modified material.
 * No additional restrictions — You may not apply legal terms or technological measures that legally restrict
 *                              others from doing anything the license permits.
 *** Notices
 * Exceptions - You do not have to comply with the license for elements of the material in the public domain or
 *              where your use is permitted by an applicable exception or limitation.
 * Warranty - No warranties are given. The license may not give you all of the permissions necessary for your
 *            intended use. For example, other rights such as publicity, privacy, or moral rights may limit how
 *            you use the material.
 */

val rattles = arrayOf(
        "You've killed me. Bastards, you've killed me! While the sun is still hot, I die!" to "Collected on Chachabah 1171, 10 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed soldier thirty-one years of age. Sample is considered questionable.",
        "The love of men is a frigid thing, a mountain stream only three steps from the ice. We are his. Oh Stormfather... we are his. It is but a thousand days, and the Everstorm comes." to "Collected on Shashahes 1171, 31 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed pregnant woman of middle years. The child did not survive.",
        "Ten orders. We were loved, once. Why have you forsaken us, Almighty! Shard of my soul, where have you gone?" to "Collected on Kakan 1171, 5 seconds pre-death, by the Silent Gatherers. Subject was a lighteyed woman in her third decade.",
        "A man stood on a cliffside and watched his homeland fall into dust. The waters surged beneath, so far beneath. And he heard a child crying. They were his own tears." to "Collected on Tanatesev 1171, 30 seconds pre-death, by the Silent Gatherers. Subject was a cobbler of some renown.",
        "I'm dying, aren't I? Healer, why do you take my blood? Who is that beside you, with his head of lines? I can see a distant sun, dark and cold, shining in a black sky." to "Collected on Jesanach 1172, 11 seconds pre-death, by the Silent Gatherers. Subject was a Reshi chull trainer. Sample is of particular note.",
        "I have seen the end, and have heard it named. The Night of Sorrows, the True Desolation. The Everstorm." to "Collected on Naneses 1172, 15 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed youth of unknown origin.",
        "I'm cold. Mother, I'm cold. Mother? Why can I still hear the rain? Will it stop?" to "Collected on Vevishes 1172, 32 seconds pre-death, by the Silent Gatherers. Subject was a lighteyed female child, approximately six years old.",
        "They are aflame. They burn. They bring the darkness when they come, and so all you can see is that their skin is aflame. Burn, burn, burn..." to "Collected on Palahishev 1172, 21 seconds pre-death, by the Silent Gatherers. Subject was a baker's apprentice.",
        "Victory! We stand atop the mount! We scatter them before us! Their homes become our dens, their lands are now our farms! And they shall burn, as we once did, in a place that is hollow and forlorn." to "Collected on Ishashan 1172, 18 seconds pre-death, by the Silent Gatherers. Subject was a lighteyed spinster of the eighth dahn.",
        "Ten people, with Shardblades alight, standing before a wall of black and white and red." to "Collected on Jesachev 1173, 12 seconds pre-death, by the Silent Gatherers. Subject an ardent member of the Silent Gatherers, overheard during his last moments.",
        "Three of sixteen ruled, but now the Broken One reigns." to "Collected on Chachanan 1173, 84 seconds pre-death, by the Silent Gatherers. Subject was a cutpurse with the wasting sickness, of partial Iriali descent.",
        "I'm standing over the body of a brother. I'm weeping. Is that his blood or mine? What have we done?" to "Collected on Vevanev 1173, 107 seconds pre-death, by the Silent Gatherers. Subject was an out-of-work Veden sailor.",
        "He must pick it up, the fallen title! The tower, the crown, and the spear!" to "Collected on Vevahach 1173, 8 seconds pre-death, by the Silent Gatherers. Subject was a prostitute of unknown background.",
        "The burdens of nine become mine. Why must I carry the madness of them all? Oh, Almighty, release me." to "Observed on Palaheses 1173, collected secondhand and later reported to the Silent Gatherers. Subject was a wealthy lighteyes.",
        "A woman sits and scratches out her own eyes. Daughter of kings and winds, the vandal." to "Collected on Palahevan 1173, 73 seconds pre-death, by the Silent Gatherers. Subject was a beggar of some renown, known for his elegant songs.",
        "Light grows so distant. The storm never stops. I am broken, and all around me have died. I weep for the end of all things. He has won. Oh, he has beaten us." to "Collected on Palahakev 1173, 16 seconds pre-death, by the Silent Gatherers. Subject was a Thaylen sailor.",
        "I hold the suckling child in my hands, a knife at his throat, and know that all who live wish me to let the blade slip. Spill its blood upon the ground, over my hands, and with it gain us further breath to draw." to "Collected on Shashanan 1173, 23 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed youth of sixteen years. Sample is of particular note.",
        "Re-Shephir, the Midnight Mother, giving birth to abominations with her essence so dark, so terrible, so consuming. She is here! She watches me die!" to "Collected on Shashabev 1173, 8 seconds pre-death, by the Silent Gatherers. Subject was a darkeyed dock-worker in his forties, father of three.",
        "The death is my life, the strength becomes my weakness, the journey has ended." to "Observed on Betabanes 1173, 95 seconds pre-death, collected secondhand and later reported to the Silent Gatherers. Subject was a scholar of some minor renown. Sample considered questionable.",
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
        "They break the land itself! They want it, but in their rage they will destroy it. Like the jealous man burns his rich things rather than let them be taken by his enemies! They come!" to "Observed circa 1173. Subject was Gadol, a member of Bridge Four.",
        "And all the world was shattered! The rocks trembled with their steps, and the stones reached toward the heavens. We die! We die!" to "Observed circa 1173. Subject was Maps, a member of Bridge Four.",
        "The day was ours, but they took it. Stormfather! You cannot have it. The day is ours. They come, rasping, and the lights fail. Oh, Stormfather!" to "Collected circa Tanat 1173 by Taravangian. Subject was a young boy.",
        "So the night will reign, for the choice of honor is life..." to "Observed circa Ishi 1173 by Taravangian. Subject was King Valam of Jah Keved.")


val archive = arrayOf(
        "As a Stoneward, I spent my entire life looking to sacrifice myself. I secretly worry that is the cowardly way. The easy way out." to "From drawer 29-5, topaz",
        "If this is to be permanent, then I wish to leave record of my husband and children. Wzmal, as good a man as any woman could dream of loving. Kmakra and Molinar, the true gemstones of my life." to "From drawer 12-15, ruby",
        "I worry about my fellow Truthwatchers." to "From drawer 8-21, second emerald",
        "We can record any secret we wish, and leave it here? How do we know that they'll be discovered? Well, I don't care. Record that then." to "From drawer 2-3, smokestone",
        "I wish to submit my formal protest at the idea of abandoning the tower. This is an extreme step, taken brashly." to "From drawer 2-22, smokestone",
        "I returned to the tower to find squabbling children, instead of proud knights, That's why I hate this place. I'm going to go chart the hidden undersea caverns of Aimia; find my maps in Akinah." to "From drawer 16-16, amethyst",
        "The disagreements between the Skybreakers and the Windrunners have grown to tragic levels. I plead with any who hear this to recognize you are not so different as you think." to "From drawer 27-19, topaz",
        "Now that we abandon the tower, can I finally admit that I hate this place? Too many rules." to "From drawer 8-1, amethyst",
        "This generation has had only one Bondsmith, and some blame the divisions among us upon this fact. The true problem is far deeper. I believe that Honor himself is changing." to "From drawer 24-18, smokestone",
        "My research into the cognitive reflections of the spren at the tower has been deeply illustrative. Some thought that the Sibling had withdrawn from men by intent- but I find counter to that theory." to "From drawer 1-1, first zircon",
        "The wilting of plants and the general cooling of the air is disagreeable, yes, but some of the tower's functions remain in place. The increased pressure, for example, persists." to "From drawer 1-1, second zircon",
        "Something is happening to the Sibling. I agree this is true, but the division among the Knights Radiant is not to blame. Our perceived worthiness is a separate issue." to "From drawer 1-1, third zircon",
        "The Edgedancers are too busy relocating the tower's servants and farmers to send a representative to record their thoughts in these gemstones.\nI'll do it for them, then. They are the ones who will be most displaced by this decision. The Radiants will be taken in by nations, but what of all these people now without homes?" to "From drawer 4-17, second topaz",
        "I am worried about the tower's protections failing. If we are not safe from the Unmade here, then where?" to "From drawer 3-11, garnet",
        "Today, I leaped from the tower for the last time. I felt the wind dance around me as I fell all the way along the eastern side, past the tower, and to the foothills below. I'm going to miss that." to "From drawer 10-1, sapphire",
        "Something must be done about the remnants of Odium's forces. The parsh, as they are now called, continue their war with zeal, even without their masters from Damnation." to "From drawer 30-20, first emerald",
        "A coalition has been formed among scholar Radiants. Our goal is to deny the enemy their supply of Voidlight; this will prevent their continuing transformations, and give us an edge in combat." to "From drawer 30-20, second emerald",
        "Our revelation is fueled by the theory that the Unmade can perhaps be captured like ordinary spren. It would require a special prison. And Melishi." to "From drawer 30-20, third emerald",
        "Ba-Ado-Mishram has somehow Connected with the parsh people, as Odium once did. She provides Voidlight and facilitates forms of power. Our strike team is going to imprison her." to "From drawer 30-20, fourth emerald",
        "We are uncertain the effects this will have on the parsh. At the very least, it should deny them forms of power. Melishi is confident, but Naze-daughter-Kuzodo warns of unintended side effects." to "From drawer 30-20, fifth emerald",
        "Surely this will bring - at long last - the end to war that the Heralds promised us." to "From drawer 30-20, final emerald",
        "As the duly appointed keepers of the perfect gems, we of the Elsecallers have taken the burden of protecting the ruby nicknamed Honor's Drop. Let it be recorded." to "From drawer 20-10, zircon",
        "The enemy makes another push toward Feverstone Keep. I wish we knew what it was that had them so interested in that area. Could they be intent on capturing Rall Elorim?" to "From drawer 19-2, third topaz",
        "Don't tell anyone. I can't say it. I must whisper. I foresaw this." to "From drawer 30-20, a particularly small emerald",
        "My spren claims that recording this will be good for me, so here I go. Everyone says I will swear the Fourth Ideal soon, and in so doing, earn my armor. I simply don't think that I can. Am I not supposed to want to help people?" to "From drawer 10-12, sapphire",
        "Good night, dear Urithiru. Good night, sweet Sibling. Good night, Radiants." to "From drawer 29-29, ruby")
        .map { it.first to (it.second to gemColorFor(it.second)) }.toTypedArray()

val diagram = arrayOf(
        "They will come you cannot stop their oaths look for those who survive when they should not that pattern will be your clue." to "From the Diagram, Coda of the Northwest Bottom Corner: Paragraph 3",
        "One danger in deploying such a potent weapon will be the potential encouragement of those exploring the Nahel bond. Care must be taken to avoid placing these subjects in situations of powerful stress unless you accept the consequences of their potential Investiture." to "From the Diagram, Foorboard 27: Paragraph 6",
        "Ah​but​they​were​left​behind​It​is​obvious​from​then​ature​of​the​bond​But​where​where​where​where​Setoff​Obvious​Realization​like​a​pricity​They​are​with​the​Shin​We​must​find​one​Can​we​make​to​use​a​Truthless​Can​we​craft​a​weapon" to "From the Diagram, Floorboard 17: Paragraph 2, every second letter starting with the first",
        "Q: For what essential must we strive? A: The essential of preservation, to shelter a seed of humanity through the coming storm. Q: What cost must we bear? A: The cost is irrelevant. Mankind must survive. Our burden is that of the species, and all other considerations are but dust by comparison." to "From the Diagram, Catechism of the Back of the Flowered Painting: Paragraph 1",
        "You must become king. Of Everything." to "From the Diagram, Tenets of Instruction, Back of the Footboard: Paragraph 1",
        "The Unmade are a deviation, a flair, a conundrum that may not be worth your time. You cannot help but think of them. They are fascinating. Many are mindless. Like the spren of human emotions, only much more nasty. I do believe a few can think, however." to "From the Diagram, Book of the 2nd Desk Drawer: Paragraph 14",
        "There is one you will watch. Though all of them have some relevance to precognition, Moelach is one of the most powerful in this regard. His touch seeps into a soul as it breaks apart from the body, creating manifestations powered by the spark of death itself. But no, this is a distraction. Deviation. Kingship. We must discuss the nature of kingship." to "From the Diagram, Book of the 2nd Desk Drawer: Paragraph 15",
        "Obviously they are fools The Desolation needs no usher It can and will sit where it wishes and the signs are obvious that the spren anticipate it doing so soon The Ancient of Stones must finally begin to crack It is a wonder that upon his will rested the prosperity and peace of a world for over four millennia" to "From the Diagram, Book of the 2nd Ceiling Rotation: Pattern 1",
        "111​825​101​112​712​491​512​101​011​141​021​511​711​210​111​217​134​483​111​071​514​254​143​410​916​149​149​341​212​254​101​012​512​710​151​910​111​234​125​511​525​121​575​511​123​410​111​291​512​106​153​4" to "From the Diagram, Book of the 2nd Ceiling Rotation: Pattern 15",
        "But who is the wanderer, the wild piece, the one who makes no sense? I glimpse at his implications, and the world opens to me. I shy back. Impossible. Is it?" to "From the Diagram, West Wall Psalm of Wonders: Paragraph 8 (Note by Adrotagia: Could this refer to Mraize?)",
        "One is almost certainly a traitor to the others." to "From the Diagram, Book of the 2nd Desk Drawer: Paragraph 27",
        "Chaos in Alethkar is, of course, inevitable. Watch carefully, and do not let power in the kingdom solidify. The Blackthorn could become an ally or our greatest foe, depending on whether he takes the path of the warlord or not. If he seems likely to sue for peace, assassinate him expeditiously. The risk of competition is too great." to "From the Diagram, Writings upon the Bedstand Lamp: Paragraph 4 (Adrotagia's 3rd translation from the original hieroglyphics)",
        "1173090605 1173090801 1173090901 1173091001 1173091004\n1173100105 1173100205 1173100401 1173100603 1173100804" to "North Wall Coda, Windowsill region: Paragraph 2 (This appears to be a sequence of dates, but their relevance is as yet unknown.)",
        "There​has​to​be​an​answer​What​is​the​answer​Stop​The​Parshendi​One​of​them​Yes​they​are​the​missing​piece​Push​for​the​Alethi​to​destroy​them​outright​before​this​one​obtains​their​power​It​will​form​abridge" to "From the Diagram, Floorboard 17: Paragraph 2, every second letter starting with the second")

@RegisterHandlers
fun registerCalloutDataHandlers() {
    addCalloutHandler("Check the Gemstone Archive") { _, _, _, message ->
        message.channel.sendRandomEmbed(message.author, "Gemstone Archives", archive)
    }

    addCalloutHandler("Ask the Silent Gatherers") { _, _, _, message ->
        message.channel.sendRandomEmbed(message.author, "Death Rattles", Color.RED, rattles)
    }

    addCalloutHandler("Consult the Diagram") { _, _, _, message ->
        message.channel.sendRandomEmbed(message.author, "The Diagram", Color.BLUE, diagram)
    }
}

