# Get Foreground Application

Sample code to make use of Accessibility Service permissions to get the package name of the application running in the foreground.
<br><br>
The Accessibility Service class is extended.<br>
When foreground application changes AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED callback is received.<br>
The package name of the current foreground is available from the accessibilityevent object.
