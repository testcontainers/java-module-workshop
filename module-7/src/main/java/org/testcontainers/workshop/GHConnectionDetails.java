package org.testcontainers.workshop;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;

public interface GHConnectionDetails extends ConnectionDetails {

	String url();

	String token();

}
