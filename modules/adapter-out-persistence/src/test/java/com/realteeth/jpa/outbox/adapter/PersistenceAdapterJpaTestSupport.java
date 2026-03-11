package com.realteeth.jpa.outbox.adapter;

import com.realteeth.jpa.PersistenceJpaTestApplication;
import com.realteeth.jpa.config.JpaConfig;
import com.realteeth.jpa.imagejob.adapter.ImageJobPersistenceAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = PersistenceJpaTestApplication.class)
@Import({
        OutboxPersistenceAdapter.class,
        ImageJobPersistenceAdapter.class,
        JpaConfig.class
})
public abstract class PersistenceAdapterJpaTestSupport {
    @Autowired
    protected OutboxPersistenceAdapter outboxPersistenceAdapter;

    @Autowired
    protected ImageJobPersistenceAdapter imageJobPersistenceAdapter;

    @Autowired
    protected TestEntityManager entityManager;

}
