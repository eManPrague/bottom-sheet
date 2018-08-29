# Bottom Sheet with two peek heights

This repository contains implementation of Google's Bottom Sheet which was modified to fully support two peek heights.

## Contents

Repository consists of two modules:
- `app` - sample application which implements required interfaces
- `sheet` - library 

## Quickstart

First of all link `sheet` module to your project.
 
```groovy
implementation project(':sheet')
```

Now let's create a layout for our view. You will need:
- `background_container` - container which will contain fragment that sheet will overlap (probably map)
- `coordinator_layout` - do not forget to include id of layout or `CoordinatorLayout` will not restore instance state
- `bottom_sheet` - our sheet will inflate into this layout
- `sheet_container` - place sheet container here

Here's sample:

```xml
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:background="@color/activity_background"
    android:layout_height="match_parent">

    <FrameLayout
        android:id="@+id/background_container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fitsSystemWindows="false"/>
        
        <android.support.design.widget.CoordinatorLayout
            xmlns:android="http://schemas.android.com/apk/res/android"
            xmlns:app="http://schemas.android.com/apk/res-auto"
            android:id="@+id/coordinator_layout"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:focusable="true"
            android:focusableInTouchMode="true">

            <LinearLayout
                android:id="@+id/bottom_sheet"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:background="@android:color/white"
                android:clipToPadding="true"
                android:orientation="vertical"
                app:layout_behavior="@string/bottom_sheet_two_states">
                

                <FrameLayout
                    android:id="@+id/sheet_container"
                    android:layout_width="match_parent"
                    android:layout_height="match_parent"
                    android:elevation="2dp"/>

            </LinearLayout>
            
        </android.support.design.widget.CoordinatorLayout>
        
    </FrameLayout>
    
</FrameLayout>
```

Then in your code initialize your sheet. You can do it manually, or you can use our handy `SheetHelper`. 
By default you'll need to just specify two peek heights - collapsed (smaller) and semi-collapsed (bigger).

```java
SheetHelper helper = new SheetsHelper.Builder(this, this)
    .setCollapsedHeight(getResources().getDimensionPixelSize(R.dimen.sheet_collapsed_height))
    .setSemiCollapsedHeight(getResources().getDimensionPixelSize(R.dimen.sheet_semicollapsed_height))
    .build();
```

Then just attach sheet with helper depending on your desires.
- `SheetHelper#init(View, BottomSheet)` - initializes with two collapsed states as defined in builder
- `SheetHelper#initSemiCollapsed(View, BottomSheet)` - initializes with only collapsed state (semi-collapsed) as defined in builder
- `SheetHelper#initCollapsed(View, BottomSheet)` - initializes with only collapsed state (collapsed) as defined in builder

```java
helper.init(mBottomSheetView, mBottomSheetBehaviour);
helper.initSemiCollapsed(mBottomSheetView, mBehaviour);
helper.initCollapsed(mBottomSheetView, mBehaviour);
```

Changing states is also really easy - image that your sheet now supports two collapsed states and you want to change it to just one bigger. No problem, call one of these:
```java
helper.animateToSemiCollapsed();
helper.restoreSemiCollapsedState();
```

Want to restore two collapsed states behaviour? Pick one of these:
```java
helper.animateToTwoStates();
helper.restoreTwoStates();
```