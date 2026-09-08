# Auto-created topics
auto.create.topics.enable=${auto_create_topics_enable}

# Replication
default.replication.factor=${default_replication_factor}
min.insync.replicas=${min_insync_replicas}

# Partitions
num.partitions=${num_partitions}

# Log retention
log.retention.hours=${log_retention_hours}
log.segment.hours=${log_segment_hours}

# Compression
compression.type=${compression_type}

# Message size
message.max.bytes=${max_message_bytes}
replica.fetch.max.bytes=${max_message_bytes + 1024}

# Performance tuning
num.network.threads=8
num.io.threads=8
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# Replication settings
replica.lag.time.max.ms=30000
replica.socket.timeout.ms=30000
replica.socket.receive.buffer.bytes=65536

# Topic management
delete.topic.enable=true

# Leader imbalance
auto.leader.rebalance.enable=true
leader.imbalance.check.interval.seconds=300
