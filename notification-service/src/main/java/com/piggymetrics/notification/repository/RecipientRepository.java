package com.piggymetrics.notification.repository;

import com.piggymetrics.notification.domain.Recipient;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecipientRepository extends CrudRepository<Recipient, String>, RecipientRepositoryCustom {

	Recipient findByAccountName(String name);

	List<Recipient> findReadyForBackup();

	List<Recipient> findReadyForRemind();

}
