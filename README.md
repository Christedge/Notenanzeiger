# Notenanzeiger
Notenanzeiger is a spike of a fullscreen PDF viewer, intended to be used by musicians to display sheet music on large tablets.

It provides a distraction free user interface which just allows to turn pages forth and back. There's no support for zoom or annotations. Since Notenanzeiger is single threaded at the moment, scrolling through the pages is not as smooth as desirable.


# Motivation
I was looking for a tool to display sheet music in PDF format on a 12" tablet. None of the existing PDF viewers or Sheet Music tools (like Mobile Sheets https://play.google.com/store/apps/details?id=com.zubersoft.mobilesheetspro or Orpheus https://play.google.com/store/apps/details?id=com.gramercy.orpheus) fulfilled my needs. Any touch on the screen should reliably turn the pages, even if I touch with multiple fingers or move the fingers a little bit while touching (which the default gesture detectors of Android so not support). After unsuccessfully lurking around Github several times to find a tool capable to fulfil my requirements, I decided to do what I have little clue about - writing my very own tool.

# Code
I'm not a professional software engineer, and Notenanzeiger was just written for my very own educational purpose. The code is no more than a conglomeration of several attempts to learn about coding in general and Android in special. Some of the goals:

* Learn how to use the native PDF renderer of Android, so it's easy to build, without any dependencies to third party libraries.
* Learn how to use Intents and ContentResolvers.
* Learn how to use the RecyclerView.
* Learn how to use the Android Storage Access Framework.
* Learn how to cache documents.
* Learn how to crop bitmaps.
* Learn about activity lifecycle management.
* Learn about motion events and gestures.
* …

# Further development
I'm not working on the code at the moment, but I'm using it on a daily basis while practicing baroque pipe organ music. In case someone wants to teach me how to refactor the code in a professional manner (e.g. applying clean code principles, rewriting it in Kotlin, making it testable etc.), do not hesitate to drop me a line.