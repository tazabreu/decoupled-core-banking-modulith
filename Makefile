create-connector:
	curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" \
		localhost:8083/connectors/ -d '{ \
			"name": "corebanking-connector", \
			"config": { \
				"connector.class": "io.debezium.connector.postgresql.PostgresConnector", \
				"tasks.max": "1", \
				"database.hostname": "corebanking-db", \
				"database.port": "5432", \
				"database.user": "corebanking_app", \
				"database.password": "c0r3b4nk1ng", \
				"database.dbname": "corebanking", \
				"database.server.name": "corebanking", \
				"schema.include.list": "public", \
				"table.include.list": "public.accounts,public.transfers,public.event_publications", \
				"plugin.name": "pgoutput", \
				"slot.name": "corebanking_slot" \
			} \
		}'

check-connector:
	curl -s localhost:8083/connectors/corebanking-connector/status | jq