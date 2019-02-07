CREATE OR REPLACE TEMPORARY VIEW wkp_table
AS
SELECT 
    parentPageId        AS parent_page_id,	
    parentRevisionId    AS parent_revision_id,	
    parentHeaderId      AS parent_header_id,
    elementId           AS element_id,	
    tableHtmlType       AS table_html_type,	
    caption             AS caption,
    html                AS html
FROM 
    tables
WHERE
    html NOT IN(
    '<dl><dd></dd></dl>',
    '<ul><li></li></ul>',
    '<table></table>',
    '<ul><li></li><li></li></ul>',
    '<ol><li></li></ol>')