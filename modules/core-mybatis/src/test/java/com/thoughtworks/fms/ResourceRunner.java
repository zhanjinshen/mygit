package com.thoughtworks.fms;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.thoughtworks.fms.core.mybatis.app.ModelModule;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionManager;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class ResourceRunner extends BlockJUnit4ClassRunner {

    @Inject
    private SqlSessionManager sqlSessionManager;

    private Injector injector;

    public TestRule cleanDatabase = (base, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            try {
                byte[] aaa = new byte[5096];
                base.evaluate();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
                throw throwable;
            } finally {
                cleanDatabase();
            }
        }
    };

    public ResourceRunner(final Class<?> clazz) throws InitializationError {
        super(clazz);
        this.injector = Guice.createInjector(asList(new ModelModule("env"),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                    }
                }, TestResourceConfig.mockModule()));

        injector.injectMembers(this);
    }

    private void cleanDatabase() throws SQLException {
        SqlSession sqlSession = sqlSessionManager.openSession();
        final PreparedStatement statement = sqlSession.getConnection().prepareStatement("SET FOREIGN_KEY_CHECKS = 0;" +
                "TRUNCATE FMS_FILE_METADATA;" +
                "SET FOREIGN_KEY_CHECKS = 1;");
        statement.setQueryTimeout(20);
        statement.execute();
        sqlSession.commit(true);
        sqlSession.flushStatements();
        sqlSession.close();
    }

    @Override
    protected List<TestRule> getTestRules(Object target) {
        List<TestRule> rules = new ArrayList<>();
        rules.add(cleanDatabase);
        rules.addAll(super.getTestRules(target));
        return rules;
    }

    @Override
    protected Object createTest() throws Exception {
        Object testClass = super.createTest();
        try {
            injector.injectMembers(testClass);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        }

        return testClass;
    }
}
