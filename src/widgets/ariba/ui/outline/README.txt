README:  ariba.ui.outline

Overview
-------------
The ariba.ui.outline package provides versatile tree/outline control for navigating a recursive
data structure (an org hierarchy, folder tree, etc).  It can be combined with a table (AWTDataTable)
to provide an OutlineTable.

Key Classes (read the javadoc comment on each class for more info):
    - AWTOutlineRepetition  - The master of the outline component tree.  Stateful -- manages state/bindings for other components
    - AWTOutlineInnerRepetition
                            - A (private) recursive component used by the AWTOutlineRepetition to render lists and sublists
    - AWTOutlineControl     - A UI control for indenting the outline items and providing a control to expand to see children

Primary Dependent:  ariba.ui.table.AWTDataTable


Tests / Examples:
--------------------
- In DemoShell
    - example/ui/outline/*


Enhancement:
- Overflow path support

Issues:
- Avoid constant sorting -- lots of garbage
    - idea: cache sorted lists on OutlineRepetition (stateful) and check for invalidation (size change)
