UPDATE environment_controllers
SET target_axis = UPPER(target_axis)
WHERE target_axis IS NOT NULL
  AND target_axis <> UPPER(target_axis);
