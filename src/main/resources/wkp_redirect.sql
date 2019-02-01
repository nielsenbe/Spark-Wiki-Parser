CREATE OR REPLACE TEMPORARY VIEW wkp_redirect
AS
SELECT 
    art.id      AS target_page_id, 
    rdr.id      AS redirect_page_id,
    rdr.title   AS redirect_title
FROM 
    items rdr
JOIN
    items art 
    ON  art.title = rdr.redirect