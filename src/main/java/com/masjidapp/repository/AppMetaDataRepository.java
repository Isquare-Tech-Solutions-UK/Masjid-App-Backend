package com.masjidapp.repository;

import com.masjidapp.entity.AppMetaData;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppMetaDataRepository extends JpaRepository<AppMetaData, UUID> {

    boolean existsByModuleNameIgnoreCaseAndValueIgnoreCase(String moduleName, String value);

    List<AppMetaData> findByModuleName(String moduleName);
}
