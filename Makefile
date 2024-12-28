run-java-jar:
	java -javaagent:./backend/tools/opentelemetry-javaagent.jar \
		-Dotel.exporter.otlp.endpoint=http://localhost:4318 \
		-Dotel.service.name=DecoupledCoreBankingModulith \
		-Dotel.logs.exporter=otlp \
		-Dotel.metrics.exporter=otlp \
		-Dotel.traces.exporter=otlp \
		-Dotel.bsp.schedule.delay=30000 \
		-Dotel.bsp.max.queue.size=4096 \
		-Dotel.bsp.max.export.batch.size=1024 \
		-Dotel.bsp.export.timeout=15000 \
		-jar ./backend/target/DecoupledCoreBankingModulith-0.0.1-SNAPSHOT.jar

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