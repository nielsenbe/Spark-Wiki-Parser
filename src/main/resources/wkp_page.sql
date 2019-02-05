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
                                           WHEN dmg.is_disambig = 1 THEN 'DISAMBIGUATION'
                                           WHEN UPPER(title) LIKE 'LIST OF%' THEN 'LIST'
                                           WHEN text_length / CAST(wiki_link_count AS double) < 5 THEN 'LIST' /* High Text to link ratio */
                                           ELSE 'ARTICLE'
                                           END                     AS page_type,
                                       revision_id                 AS revision_id,
                                       revision_date               AS revision_date,
                                       hdr.text_length             AS text_length,
                                       hdr.wiki_link_count         AS wiki_link_count,
                                       lnk.incoming_wiki_link_count AS incoming_wiki_link_count,
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
                                   LEFT JOIN(
                                       SELECT
                                           destination_page_id,
                                           COUNT(*) as incoming_wiki_link_count
                                       FROM
                                           wkp_link_wiki
                                       GROUP BY
                                           destination_page_id
                                       ) lnk
                                       ON lnk.destination_page_id = pge.id
                                   LEFT JOIN(
                                       SELECT
                                           parent_page_id,
                                           MAX(CASE WHEN UPPER(template_type) = 'DISAMBIGUATION' THEN 1 ELSE 0 END) AS is_disambig
                                       FROM
                                           wkp_template
                                       GROUP BY
                                           parent_page_id
                                       ) as dmg
                                       ON  dmg.parent_page_id = pge.id