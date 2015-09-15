# p3superduo-alexandria-master

Alexandria retrieves book information from a Google API. All titles, cover images and author information originate from publicly available Google APIs.\n\nThis app was originally created by Sascha Jaschke and modified by Kyle Parker as part of the Udacity Android Developer Nanodegree program.
  
Changelog:
        * Updated navigation drawer to more closely follow Android design guidelines
        * Updated overall app design to incorporate Material Design patterns (floating action button, coordinator layout, collapsing toolbar, etc)
        * Updated portions of the app to use objects and the content provider rather than direct cursor access
        * Updated to use parcels to handle device orientation and data access
        * Updated the About dialog to reference the changes made to the app
        * Updated the tablet check to utilize the resource folders rather than checking the screen size
        * Updated the book detail activity to more closely follow standard app interaction
        * Added scanning functionality to add book using a barcode
        * Added a check to determine if the device is offline, prior to searching for a book
        * Added error handling to check the ISBN for length and to verify it contains only digits
        * Added option in Settings to remove all books from the user\'s library
        * Swapped out existing image loader with Picasso image library
        * Corrected problem with sharing a book
        * Corrected the back button behavior by reworking the structure of the app
        * Corrected problem with entering a book and the device rotation
        * Removed option to select starting screen - with the addition of the floating action button, this feature was no longer needed and does not follow a typical app structure
