package com.example.bankcards.config;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import io.github.perplexhub.rsql.RSQLCommonSupport;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsqlVisitorConfig {

    @PostConstruct
    public void blacklistRSQLProps() {
        RSQLCommonSupport.addPropertyBlacklist(User.class, "password");
        RSQLCommonSupport.addPropertyBlacklist(Card.class, "cardNumber");
    }
}
