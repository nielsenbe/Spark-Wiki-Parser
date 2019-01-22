CREATE OR REPLACE TEMPORARY VIEW wkp_page
AS
SELECT
    id,
    title,
    CASE
        WHEN title LIKE '%:%' AND title LIKE '%)'
            THEN SPLIT(SPLIT(title, '[:]')[1], '[(]')[0]
        WHEN title LIKE '%:%'
            THEN SPLIT(title, '[:]')[1]
        WHEN title LIKE '%)' 
            THEN SPLIT(title, '[(]')[0]
            ELSE title
            END AS entity,
    CASE 
        WHEN title LIKE '%)' 
            THEN TRANSLATE(SPLIT(title, '[(]')[1], ')', '') 
            END AS sense,
    name_space          AS name_space_num,
    CASE
        WHEN name_space = 0 THEN 'ARTICLE'
        WHEN name_space = 1 THEN 'TALK'
        WHEN name_space = 2 THEN 'USER'
        WHEN name_space = 4 THEN 'WIKIPEDIA'
        WHEN name_space = 6 THEN 'FILE'
        WHEN name_space = 8 THEN 'MEDIAWIKI'
        WHEN name_space = 10 THEN 'TEMPLATE'
        WHEN name_space = 12 THEN 'HELP'
        WHEN name_space = 14 THEN 'CATEGORY'
        WHEN name_space = 100 THEN 'PORTAL'
        WHEN name_space = 108 THEN 'BOOK'
        WHEN name_space = 118 THEN 'DRAFT'
        WHEN name_space = 446 THEN 'EDUCATION'
        WHEN name_space = 710 THEN 'TIMEDTEXT'
        WHEN name_space = 828 THEN 'MODULE'
        WHEN name_space = 2300 THEN 'GADGET'
        WHEN name_space = 2302 THEN 'GADGET DEFINITION'
        WHEN name_space = -1  THEN 'PECIAL'
        WHEN name_space = -2  THEN 'MEDIA'
        ELSE 'OTHER'
        END                 AS name_space_text,
    CASE
        WHEN name_space = 1 THEN 'TALK'
        WHEN name_space = 2 THEN 'USER'
        WHEN name_space = 4 THEN 'WIKIPEDIA'
        WHEN name_space = 6 THEN 'FILE'
        WHEN name_space = 8 THEN 'MEDIAWIKI'
        WHEN name_space = 10 THEN 'TEMPLATE'
        WHEN name_space = 12 THEN 'HELP'
        WHEN name_space = 14 THEN 'CATEGORY'
        WHEN name_space = 100 THEN 'PORTAL'
        WHEN name_space = 108 THEN 'BOOK'
        WHEN name_space = 118 THEN 'DRAFT'
        WHEN name_space = 446 THEN 'EDUCATION'
        WHEN name_space = 710 THEN 'TIMEDTEXT'
        WHEN name_space = 828 THEN 'MODULE'
        WHEN name_space = 2300 THEN 'GADGET'
        WHEN name_space = 2302 THEN 'GADGET DEFINITION'
        WHEN name_space = -1  THEN 'PECIAL'
        WHEN name_space = -2  THEN 'MEDIA'
        WHEN UPPER(title) LIKE '%DISAMBIGUATION%' THEN 'DISAMBIGUATION'
        WHEN UPPER(title) LIKE '%CATEGORY%' THEN 'CATEGORY'
        WHEN UPPER(title) LIKE 'LIST OF%' THEN 'LIST'
        ELSE 'ARTICLE'
        END                     AS page_type,
    revision_id                 AS revision_id,
    revision_date               AS revision_date,
    hdr.text_length             AS text_length,
    hdr.wiki_link_count         AS wiki_link_count,
    hdr.template_count          AS template_count,
    hdr.cleanup_template_count  AS cleanup_template_count
FROM
    wkp_page_simple pge
LEFT JOIN(
    SELECT
        parent_page_id,
        SUM(text_length)            AS text_length,
        SUM(wiki_link_count)        AS wiki_link_count,
        SUM(template_count)         AS template_count,
        SUM(cleanup_template_count) AS cleanup_template_count
    FROM
        wkp_header
    GROUP BY
        parent_page_id
    ) hdr
    ON  hdr.parent_page_id = pge.id
    
    