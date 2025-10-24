package com.piggymetrics.notification.repository;

import com.piggymetrics.notification.domain.Recipient;
import java.util.List;

public interface RecipientRepositoryCustom {

	List<Recipient> findReadyForBackup();

	List<Recipient> findReadyForRemind();
}
