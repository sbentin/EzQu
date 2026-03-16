# EzQu

EzQu is short for "Easy Query" and provides a straightforward solution for accessing relational databases using pure Java. The tool is designed to be a comprehensive solution for working with relational databases, offering several advantages over traditional JDBC. Unlike JDBC, which requires writing "String"-based SQL queries, EzQu can be easily integrated into Java applications since it is written in pure Java. Additionally, EzQu uses a fluent API that enables type checking and IDE auto-complete. One of the key benefits of EzQu is its ability to provide full protection against SQL injection, ensuring the security of your database operations.

* [_A glimpse of what ezQu can do go_](https://tm.centimia.com/ezqu/code-examples)
* [_EzQu Full Documentation_](https://tm.centimia.com/ezqu)

EzQu originated in 2008 as a fork of JaQu, a tool developed by Thomas Mueller, the author of H2 Database. After jaQu was abandoned due to a lack of attention, EzQu forked the project and has since undergone significant development, following a different path from the original JaQu project. Today, EzQu is a well-maintained, supported, and extended library that has been deployed in large projects using MariaDB, Oracle, PostgresDB, DB2, MS SqlServer, and H2.

EzQu is an Object Relational Mapping (ORM) tool that enables pure Java grammar. With EzQu, you can persist objects into the database with One2One, One2Many, Many2One, and Many2Many relationships. You can also control the loading of your relationships, whether it be lazy or eager, and manage primary key relations.

When working with a relational database (or any non-Object DB), one of the challenges is mapping between the columns of the database row and the fields of the object. This is where Object Relational Mapping (ORM) comes in. There have been many ORM solutions over time, each with its advantages and disadvantages. Some developers have chosen to forgo ORM tools and instead use JDBC directly because these tools are often too inflexible and can confine them to strict rules or a specific DSL for querying the database, which may not meet all their needs. However, EzQu is different from other ORM tools, offering greater flexibility and versatility to meet developers' needs.

Here are some of the features:

* Support for ORACLE, SQL SERVER, DB/2, MARIA DB, MYSQL, H2, POSTGRESQL  databases.
* Support One2One, One2Many, Many2One, Many2Many relationships (But only with single primary keys for now), in select, insert, update & delete.
* Supports transient fields (i.e field you don't want to work with persistence)
* Lazy loading of o2o, o2m, m2m relationships
* Depth Control - Control over how deeply the object graph is persisted.
* Supports cascade deletes on relationships
* Works with private fields as well as public fields
* API for insert, update, delete of pojo's (or annotated pojo's).
* Fluent API for select, update, and delete. Write code like the following db.from(p).set(p.name, value).where(p.field).is(value).update(), db.from(p).where(p.getName()).is("jhon").select()
* Writing a primary-Key select: db.from(p).primaryKey().is(value).select()
* JTA Transaction support

