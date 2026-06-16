-- ShedLock distributed lock table.
-- Required so @SchedulerLock-annotated cron batch jobs execute on exactly one instance
-- once the API server runs behind a load balancer / multiple replicas.
-- Standard JdbcTemplateLockProvider schema (net.javacrumbs.shedlock).

CREATE TABLE shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
