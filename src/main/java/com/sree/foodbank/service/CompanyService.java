package com.sree.foodbank.service;

import com.sree.foodbank.dao.CompanyDao;
import com.sree.foodbank.model.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CompanyService {
    private final CompanyDao companyDao;

    @Autowired
    public CompanyService(@Qualifier("Dao") CompanyDao companyDao) {
        this.companyDao = companyDao;
    }

    public int addCompany(Company company) {
        return companyDao.insertCompany(company);
    }

    public List<Company> getAllCompanies() {
        return companyDao.selectAllCompanies();
    }

    public Optional<Company> getCompanybyId(UUID id) {
        return companyDao.selectCompanyById(id);
    }

    public int deleteCompany(UUID id) {
        return companyDao.deleteCompanybyId(id);
    }

    public int updateCompany(UUID id, Company newCompany) {
        return companyDao.updateCompnaybyId(id, newCompany);
    }
}
