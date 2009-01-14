package youtube;

import ariba.ui.aribaweb.core.AWComponent
import ariba.ui.table.AWTDisplayGroup
import ariba.ui.table.AWTCSVDataSource

class YouTubeBrowser extends AWComponent
{
    public AWTDisplayGroup displayGroup;
    def categories, category, video, entry;

    public void init() {
        super.init();
        categories = AWTCSVDataSource.dataSourceForPath("feedlist.csv", this).fetchObjects()
        category = categories[0]
    }

    def feed () {
        if (!category.feed) {
            def doc = new XmlSlurper().parseText(new URL(category.url).text)
            category.feed = doc.entry.collect() { e ->
                [full:e,
                 title: e.title,
                 url: blankToNull(e.group.content.find { it.@type =="application/x-shockwave-flash" }?.@url),
                 thumbnail: e.group.thumbnail[0] ]
            }
        }
        return category.feed;
    }

    def blankToNull (s) { (s == "") ? null : s; }
}
