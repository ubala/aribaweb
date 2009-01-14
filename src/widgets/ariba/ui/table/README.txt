README:  ariba.ui.table

See also ariba.ui.outline.

Overview
-------------
The ariba.ui.table package provides a rich, configurable, Ariba-branded data table.
(A distant ancestor of Ariba Network's ANXDataTable).

Key Classes (read the javadoc comment on each class for more info):
    - AWTDisplayGroup       - The row set manager for the table.  Sorting, pagination, selection, ...
    - AWTDataTable          - The AWComponent for the table.  Contains many Column children
    - AWTColumn             - The primary Column subclass for rendering a data column
    - AWTButtonArea         - A "column" for the button row area above and below the table
    - AWTRowDetail          - A "column" for the long (optional) detail area under each row
    - AWTSortHeading        - Control for column header to set sorting on AWTDisplayGroup of the table
    - AWTBatchNavigationBar - Batch navigation control used by AWTDataTable
    - AWTSortOrdering       - A key/direction pair to specify a sort (passed to AWTDisplayGroup).


Tests / Examples:
--------------------
- In DemoShell
    - sample/simple/WidgetSamples.awz/*
    - example/ui/outline/*

Migration Notes:
--------------------
- Should Stay in demoshell  -->  Move into ariba.ui.demoshell
    - AWLoadXML
    - XMLFactory
    - AWTCSVDataSource, and AWTCSVData

- Port tests from .htm/javascript into WidgetsTest


Issues:
----------------------

TO DO:
------------------------

Desirable Enhancements:
-------------------------
- Replace AWTColumn ContainerElement subclasses with AWComponents -- have AWTDataTable manage
  component references.
- Beef up DataSource interface to support sorting / fetching instead of just in memory sort.
- Replace "options" menu hyperlink with a nice table icon
- *push* groupByColumn?  Rename "groupByColumn" to "defaultGroupByColumn"?
- Update AWTBatchNavigation bar to new UCDG look


Clean Up:
-------------------------
- [done] Factor string constants (primarily for bindings) into variables.
- eliminate redundant AWTColumn bindings that can be accomplished with styles
- add style binding to AWTDataTable and clean up redundant bindings?
- roll hard coded styles on columns and table into class

Bugs:
-----------
- Excel export
    - broken for tables with stateful column content (elementId mismatch)
    - broken for outline tables (elementId mismatch)
    - should suppress expando icons, etc when exporting (maybe set "exporting" flag on environment).

