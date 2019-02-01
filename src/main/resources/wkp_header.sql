CREATE OR REPLACE TEMPORARY VIEW wkp_header
AS
SELECT
    hdr.parentPageId    AS parent_page_id,
    hdr.parentRevisionId AS parent_revision_id,
    hdr.headerId        AS header_id,
    hdr.title           AS title,
    hdr.level           AS header_level,
    CASE WHEN UPPER(title) IN (
        "REFERENCES", 
        "EXTERNAL LINKS", 
        "SEE ALSO", 
        "NOTES", 
        "LICENSING", 
        "BIBLIOGRAPHY", 
        "FURTHER READING", 
        "SOURCES", 
        "FOOTNOTES", 
        "PUBLICATIONS", 
        "USERS", 
        "LINKS") 
        THEN TRUE 
        ELSE FALSE 
        END                                  AS is_ancillary,
    COALESCE(txt.text_length, 0)             AS text_length,
    COALESCE(lnk.wiki_link_count, 0)         AS wiki_link_count,
    COALESCE(tmp.template_count, 0)          AS template_count,
    COALESCE(tmp.cleanup_template_count, 0)  AS cleanup_template_count
FROM 
    headers hdr
LEFT JOIN(
    SELECT
        parent_page_id,
        parent_header_id,
        COUNT(*)  AS wiki_link_count
    FROM
        wkp_link_wiki
    GROUP BY
        parent_page_id,
        parent_header_id
    ) lnk
    ON  lnk.parent_page_id = hdr.parentPageId
    AND lnk.parent_header_id = hdr.headerId
    
LEFT JOIN(
    SELECT
        parent_page_id,
        parent_header_id,
        SUM(text_length)  AS text_length
    FROM
        wkp_text
    GROUP BY
        parent_page_id,
        parent_header_id
    ) txt
    ON  txt.parent_page_id = hdr.parentPageId
    AND txt.parent_header_id = hdr.headerId
    
LEFT JOIN(
    SELECT
        parent_page_id,
        parent_header_id,
        SUM(CASE WHEN UPPER(template_type) IN (
            "CLEANUP", 
            "CLEANUP AFD", 
            "CLEANUP SECTION",
			"CLEANUP-REORGANIZE",
            "CLEANUP-REWRITE", 
            "REQUIRES ATTENTION", 
			"CLEANUP TRANSLATION",
			"CLEANUP-PR",
            "COPY EDIT", 
			"COPYEDIT",
            "COPY EDIT-SECTION") 
            THEN 1 
            ELSE 0 
            END)        AS cleanup_template_count,
        COUNT(*)  AS template_count
    FROM
        wkp_template
    GROUP BY
        parent_page_id,
        parent_header_id
    ) tmp
    ON  tmp.parent_page_id = hdr.parentPageId
    AND tmp.parent_header_id = hdr.headerId
	