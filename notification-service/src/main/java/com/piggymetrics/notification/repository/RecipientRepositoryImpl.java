package com.piggymetrics.notification.repository;

import com.piggymetrics.notification.domain.NotificationSettings;
import com.piggymetrics.notification.domain.NotificationType;
import com.piggymetrics.notification.domain.Recipient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RecipientRepositoryImpl implements RecipientRepositoryCustom {

	private final MongoTemplate mongoTemplate;

	public RecipientRepositoryImpl(MongoTemplate mongoTemplate) {
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public List<Recipient> findReadyForBackup() {
		return findReadyFor(NotificationType.BACKUP);
	}

	@Override
	public List<Recipient> findReadyForRemind() {
		return findReadyFor(NotificationType.REMIND);
	}

	private List<Recipient> findReadyFor(NotificationType notificationType) {
		Query activeNotificationsQuery = Query.query(Criteria
				.where(field(notificationType, "active"))
				.is(true));

		return mongoTemplate.find(activeNotificationsQuery, Recipient.class)
				.stream()
				.filter(recipient -> shouldSendNotification(recipient, notificationType))
				.collect(Collectors.toList());
	}

	private boolean shouldSendNotification(Recipient recipient, NotificationType type) {
		Map<NotificationType, NotificationSettings> scheduledNotifications = recipient.getScheduledNotifications();
		if (scheduledNotifications == null) {
			return false;
		}

		NotificationSettings settings = scheduledNotifications.get(type);
		if (settings == null || !Boolean.TRUE.equals(settings.getActive())) {
			return false;
		}

		Date lastNotified = settings.getLastNotified();
		if (lastNotified == null) {
			return true;
		}

		Instant nextNotificationThreshold = lastNotified.toInstant()
				.plus(settings.getFrequency().getDays(), ChronoUnit.DAYS);

		return !nextNotificationThreshold.isAfter(Instant.now());
	}

	private static String field(NotificationType notificationType, String suffix) {
		return String.format("scheduledNotifications.%s.%s", notificationType.name(), suffix);
	}
}
