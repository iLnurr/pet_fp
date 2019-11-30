object queries {
  val createDB: String = "create database if not exists tradegoria"
  val createProducts: String =
    """
      |create table if not exists tradegoria.modx_ms2_products
      |(
      |    id             bigint primary key auto_increment,
      |    article        varchar(50),
      |    price          decimal(12, 2),
      |    oldPrice       decimal(12, 2),
      |    weight         decimal(13, 3),
      |    image          varchar(255),
      |    thumb          varchar(255),
      |    vendor         int(10),
      |    made_in        varchar(100),
      |    new            tinyint(1),
      |    popular        tinyint(1),
      |    favorite       tinyint(1),
      |    tags           text,
      |    color          text,
      |    size           text,
      |    source         int(10),
      |    area           int(11),
      |    place_area     int(11),
      |    rooms          int(11),
      |    sea_distance   int(11),
      |    sea_view       tinyint(1),
      |    furniture      tinyint(1),
      |    first_line     tinyint(1),
      |    economy        tinyint(1),
      |    hot            tinyint(1),
      |    exclusive      tinyint(1),
      |    sold           tinyint(1),
      |    video          varchar(255),
      |    old_article    varchar(50),
      |    owner_fio      varchar(255),
      |    owner_contacts varchar(500),
      |    comment        varchar(500),
      |    partner        varchar(255),
      |    sort           int(11),
      |    sort_hot       int(11),
      |    for_sale       tinyint(1),
      |    for_rent       tinyint(1),
      |    for_longrent   tinyint(1),
      |    for_change     tinyint(1)
      |);
      |""".stripMargin

  val createSiteContent: String =
    """
      |create table if not exists tradegoria.modx_site_content
      |(
      |    id                    bigint primary key auto_increment,
      |    type                  varchar(20),
      |    contentType           varchar(50),
      |    pagetitle             varchar(255),
      |    longtitle             varchar(255),
      |    description           varchar(255),
      |    alias                 varchar(255),
      |    link_attributes       varchar(255),
      |    published             tinyint(1),
      |    pub_date              int(20),
      |    unpub_date            int(20),
      |    parent                int(10),
      |    isfolder              tinyint(1),
      |    introtext             text,
      |    content               mediumtext,
      |    richtext              tinyint(1),
      |    template              int(10),
      |    menuindex             int(10),
      |    searchable            tinyint(1),
      |    cacheable             tinyint(1),
      |    createdby             int(10),
      |    createdon             int(20),
      |    editedby              int(10),
      |    editedon              int(20),
      |    deleted               tinyint(1),
      |    deletedon             int(20),
      |    deletedby             int(10),
      |    publishedon           int(20),
      |    publishedby           int(10),
      |    menutitle             varchar(255),
      |    donthit               tinyint(1),
      |    privateweb            tinyint(1),
      |    privatemgr            tinyint(1),
      |    content_dispo         tinyint(1),
      |    hidemenu              tinyint(1),
      |    class_key             varchar(100),
      |    context_key           varchar(100),
      |    content_type          int(11),
      |    uri                   text,
      |    uri_override          tinyint(1),
      |    hide_children_in_tree tinyint(1),
      |    show_in_tree          tinyint(1),
      |    properties            mediumtext
      |);
      |""".stripMargin

}
