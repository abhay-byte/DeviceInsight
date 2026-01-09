# Fix Workflow

This document outlines the steps to fix issues in the project.

## Steps to Fix an Issue

1. **Identify the Issue**: Clearly understand the problem by reviewing the issue description, logs, or error messages.

2. **Reproduce the Issue**: Ensure you can reproduce the issue locally or in a testing environment.

3. **Implement the Fix**:
   - Make the necessary changes to the codebase.
   - Follow the project's coding standards and best practices.

4. **Test the Fix**:
   - Build the project: `./gradlew build`
   - Install the app on a device: `./gradlew installDebug`
   - Run the app on the device: `adb shell am start -n com.ivarna.deviceinsight/com.ivarna.deviceinsight.MainActivity`
   - Manually verify the fix on the device.

5. **Commit Changes**:
   ```bash
   git add .
   git commit -m "Fix: Brief description of the fix"
   ```

6. **Push Changes**:
   ```bash
   git push origin main
   ```

## Best Practices

- Keep the fix focused on the specific issue.
- Write clear and concise commit messages.
- Ensure all tests pass before submitting the changes.
- Document any changes that affect the project's behavior or API.