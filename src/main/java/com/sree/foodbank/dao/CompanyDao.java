package com.sree.foodbank.dao;

import com.sree.foodbank.model.Company;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyDao {
    int insertCompany(UUID id, Company company);

    default int insertCompany(Company company) {
        UUID id = UUID.randomUUID();
        return insertCompany(id, company);
    }

    List<Company> selectAllCompanies();

    Optional<Company> selectCompanyById(UUID id);

    int deleteCompanybyId(UUID id);

    int updateCompnaybyId(UUID id, Company company);
}
