package com.sree.foodbank.dao;

import com.sree.foodbank.model.Company;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository("Dao")
public class CompanyDataAccessService implements CompanyDao {
    private static List<Company> DB = new ArrayList<>();

    @Override
    public int insertCompany(UUID id, Company company) {
        DB.add(new Company(id, company.getName(), company.getFood()));
        return 1;
    }

    @Override
    public List<Company> selectAllCompanies() {
        return DB;
    }

    @Override
    public Optional<Company> selectCompanyById(UUID id) {
        return DB.stream()
                .filter(company -> company.getId().equals(id))
                .findFirst();
    }

    @Override
    public int deleteCompanybyId(UUID id) {
        Optional<Company> companyMaybe = selectCompanyById(id);
        if (companyMaybe.isEmpty()) {
            return 0;
        }
        DB.remove(companyMaybe.get());
        return 1;
    }

    @Override
    public int updateCompnaybyId(UUID id, Company update) {
        return selectCompanyById(id)
                .map(c -> {
                int indexUpdate = DB.indexOf(c);
                if (indexUpdate >= 0) {
                    DB.set(indexUpdate, new Company(id, update.getName(), update.getFood()));
                    return 1;
                }
                return 0;
                })
                .orElse(0);
    }

}
