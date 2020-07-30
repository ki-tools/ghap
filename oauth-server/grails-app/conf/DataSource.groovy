dataSource {
    pooled = true
    jmxExport = true
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
//    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    singleSession = true // configure OSIV singleSession mode
    flush.mode = 'manual' // OSIV session flush mode outside of transactional context
}

// environment specific settings
environments {
    development {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop', 'update', 'validate', ''
            url = "jdbc:h2:mem:devDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:mem:testDb;MVCC=TRUE;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE"
        }
    }
    devtest {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            pooled = true
            url = "jdbc:postgresql://gg1s7w9qe1dkocf.cxe9zrjxx6sa.us-east-1.rds.amazonaws.com:5432/OAuthDB"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgreSQL9Dialect
            username = "ghap"
            password = ""
            properties {
                maxActive = 50
                maxIdle = 25
                minIdle = 5
                initialSize = 10
                maxWait = 10000
                minEvictableIdleTimeMillis=1800000
                timeBetweenEvictionRunsMillis=1800000
                numTestsPerEvictionRun=3
                testOnBorrow=true
                testWhileIdle=true
                testOnReturn=true
                validationQuery="SELECT 1"
            }
        }
    }
    samba {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            pooled = true
            url = "jdbc:postgresql://ggxdd3fbe2fdri.cpawdlyazfmj.us-east-1.rds.amazonaws.com:5432/OAuthDB"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgreSQL9Dialect
            username = "ghap"
            password = ""
            properties {
                maxActive = 50
                maxIdle = 25
                minIdle = 5
                initialSize = 10
                maxWait = 10000
                minEvictableIdleTimeMillis=1800000
                timeBetweenEvictionRunsMillis=1800000
                numTestsPerEvictionRun=3
                testOnBorrow=true
                testWhileIdle=true
                testOnReturn=true
                validationQuery="SELECT 1"
            }
        }
    }
    production {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            pooled = true
            url = "jdbc:postgresql://ggmpgdc53vy0i1.cxe9zrjxx6sa.us-east-1.rds.amazonaws.com:5432/OAuthDB"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgreSQL9Dialect
            username = "ghap"
            password = ""
            properties {
                maxActive = 50
                maxIdle = 25
                minIdle = 5
                initialSize = 10
                maxWait = 10000
                minEvictableIdleTimeMillis=1800000
                timeBetweenEvictionRunsMillis=1800000
                numTestsPerEvictionRun=3
                testOnBorrow=true
                testWhileIdle=true
                testOnReturn=true
                validationQuery="SELECT 1"
            }
        }
    }
    qa {
        dataSource {
            dbCreate = "update" // one of 'create', 'create-drop', 'update', 'validate', ''
            pooled = true
            url = "jdbc:postgresql://gg1rqiym72iiqcb.cqra4wbztgtm.us-east-1.rds.amazonaws.com:5432/OAuthDB"
            driverClassName = "org.postgresql.Driver"
            dialect = org.hibernate.dialect.PostgreSQL9Dialect
            username = "ghap"
            password = ""
            properties {
                maxActive = 50
                maxIdle = 25
                minIdle = 5
                initialSize = 10
                maxWait = 10000
                minEvictableIdleTimeMillis=1800000
                timeBetweenEvictionRunsMillis=1800000
                numTestsPerEvictionRun=3
                testOnBorrow=true
                testWhileIdle=true
                testOnReturn=true
                validationQuery="SELECT 1"
            }
        }
    }
}
