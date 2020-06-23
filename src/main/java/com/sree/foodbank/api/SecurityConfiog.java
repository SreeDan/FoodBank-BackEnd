package com.sree.foodbank.api;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

public class SecurityConfiog extends WebSecurityConfigurerAdapter {
    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .antMatcher("/user").authorizeRequests()
                .antMatchers("/").permitAll()
                .and()
                .oauth2Login();
    }
}
