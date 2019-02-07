CREATE OR REPLACE TEMPORARY VIEW wkp_template_param
AS
SELECT 
    tmp.parentPageId        AS parent_page_id,
    tmp.parentRevisionId    AS parent_revision_id,
    tmp.parentHeaderId      AS parent_header_id,
    tmp.elementId           AS element_id, 
    params._1               AS param_name, 
    params._2               AS param_value 
FROM 
    templates TMP 
LATERAL VIEW explode(parameters) AS params