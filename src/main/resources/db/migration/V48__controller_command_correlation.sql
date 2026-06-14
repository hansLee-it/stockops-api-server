-- Correlation id links a published controller command to the ACK echoed back by Sensimul.
-- Null for legacy HTTP-path commands.
ALTER TABLE controller_commands ADD COLUMN correlation_id VARCHAR(64);
CREATE INDEX idx_controller_commands_correlation ON controller_commands (correlation_id);
