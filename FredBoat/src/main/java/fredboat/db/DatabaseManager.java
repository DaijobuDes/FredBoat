/*
 * MIT License
 *
 * Copyright (c) 2016 Frederik Ar. Mikkelsen
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fredboat.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fredboat.db.entities.GuildConfig;
import fredboat.db.entities.TCConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.json.JSONObject;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

public class DatabaseManager {

    private static final Map<Thread, EntityManager> EM_MAP = new ConcurrentHashMap<>();
    private static EntityManagerFactory emf;
    private static final Map<String, GuildConfig> GUILD_CONFIGS = new HashMap<>();

    public static void startup(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        DataSource dataSource = new HikariDataSource(config);

        Properties properties = new Properties();
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.connection.driver_class", "org.postgresql.Driver");

        LocalContainerEntityManagerFactoryBean emfb = new LocalContainerEntityManagerFactoryBean();
        emfb.setDataSource(dataSource);
        emfb.setPackagesToScan("fredboat.db.entities");
        emfb.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        emfb.setJpaProperties(properties);
        emfb.setPersistenceUnitName("fredboat.test");
        emfb.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        emfb.afterPropertiesSet();

        emf = emfb.getObject();
    }

    public static void initBotEntities(JDA jda){
        EntityManager em = getEntityManager();

        em.getTransaction().begin();

        System.out.println(jda.getGuilds());

        for (Guild guild : jda.getGuilds()) {
            GuildConfig gc = em.find(GuildConfig.class, Long.parseLong(guild.getId()));
            if (gc == null) {
                System.err.println("NEW");
                gc = new GuildConfig();
                em.persist(gc);
            }
        }

        em.getTransaction().commit();
    }

    static EntityManager getEntityManager() {
        EntityManager em = EM_MAP.get(Thread.currentThread());

        if (em == null) {
            if(emf == null) {
                throw new DatabaseNotReadyException();
            }
            em = emf.createEntityManager();
            EM_MAP.put(Thread.currentThread(), em);
        }

        return em;
    }

    //TODO: Remove this
    /*public static GuildConfig getGuildConfig(Guild guild) {
        return GUILD_CONFIGS.get(guild.getId());
    }

    public static TCConfig getTextChannelConfig(TextChannel tc) {
        GuildConfig gc = getGuildConfig(tc.getGuild());

        if (gc != null) {
            long tcId = Long.getLong(tc.getId());
            for (TCConfig tcc : gc.getTextChannels()) {
                if (tcc.getTextChannelId() == tcId) {
                    return tcc;
                }
            }
        }

        return null;
    }

    public static void persistGuildConfig(GuildConfig gc, boolean commit) {
        EntityManager em = getEntityManager();
        if (commit) {
            em.getTransaction().begin();
        }

        em.persist(gc);
        GUILD_CONFIGS.put(Long.toString(gc.getGuildId()), gc);

        if (commit) {
            em.getTransaction().commit();
        }
    }

    public static void persistTextChannelConfig(TCConfig tcc, boolean commit) {
        EntityManager em = getEntityManager();
        if (commit) {
            em.getTransaction().begin();
        }

        em.persist(tcc);
        tcc.getGuildConfiguration().addTextChannel(tcc);

        if (commit) {
            em.getTransaction().commit();
        }
    }

    public static void remove(GuildConfig gc, boolean commit){
        EntityManager em = getEntityManager();
        if (commit) {
            em.getTransaction().begin();
        }

        em.remove(gc);
        GUILD_CONFIGS.remove(Long.toString(gc.getGuildId()));
        for(TCConfig tcc : gc.getTextChannels()){
            gc.removeTextChannel(tcc);
            em.remove(tcc);
        }

        if (commit) {
            em.getTransaction().commit();
        }
    }

    public static void remove(TCConfig tcc, boolean commit){
        EntityManager em = getEntityManager();
        if (commit) {
            em.getTransaction().begin();
        }

        em.remove(tcc);
        tcc.getGuildConfiguration().removeTextChannel(tcc);

        if (commit) {
            em.getTransaction().commit();
        }
    }*/

}