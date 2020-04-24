# Get Foreground Application

Sample code to make use of Accessibility Service permissions to get the package name of the application running in the foreground.
<br><br>
The Accessibility Service class is extended.<br>
When foreground application changes AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED callback is received.<br>
The package name of the current foreground is available from the accessibilityevent object.

StackOverflow answers<br>
[How to monitoring app swaping in foreground?](https://stackoverflow.com/questions/10826579/how-to-monitoring-app-swaping-in-foreground/61414352#61414352)<br>
[How do I get active window that is on foreground?](https://stackoverflow.com/questions/23504217/how-do-i-get-active-window-that-is-on-foreground/61414163#61414163)
