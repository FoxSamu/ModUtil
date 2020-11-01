# ModUtil
ModUtil is a Gradle plugin for Minecraft Forge mods, providing some small utilities.

## Installation
In your gradle buildscript, at the very top in the `buildscript` block, add this:
```groovy
repositories {
    // Include my maven repository
    maven { url "https://maven.shadew.net/" }
}

dependencies {
    // Include ModUtil as buildscript dependency
    classpath "net.shadew:modutil:1.0-beta.4"
}
```
Even though Forge tells you not to edit the `buildscript` block, it's perfectly safe to add ModUtil as a dependency there, it will not break your build since 1) you won't remove the ForgeGradle dependency and 2) ModUtil depends on ForgeGradle so even if you remove the ForgeGradle dependency it will still be added.

## Usage
You can configure the plugin with the `modutil` configuration block, or by configuring each task separately.
```groovy
modutil {
    // Configuration goes here
}
```

There are three main tools ModUtil provides:
- Constant injection into Java code
- Package shading
- Changelog generation into several different formats

### Constant injection
ModUtil can inject constants into your Java code based on variables in the buildscript. To use constant injection, you first want to make some annotation somewhere in your mod's code that specifies a constant injection field. What about `DynamicConstant`:
```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface DynamicConstant {
    String value();
}
```
The annotation must have a `value` parameter: this parameter specifies the name of a constant you can define in your buildscript. To use this annotation as the constant injection annotation, specify it in the `modutil` configuration block:
```groovy
modutil {
    constantAnnotation 'package.containing.your.DynamicConstant'
}
```
Now that you have configured your annotation, you can specify some constants in your java code:
```java
@DynamicConstant("myconstant")
public static final String MYCONSTANT = "typesomethingrandomhere";
```
Now there's one thing left: specifying your constant in the buildscript. You can specify constants in several ways, they all go into the `modutil` block. The first way is to add a single constant:
```groovy
modutil {
    constantAnnotation 'package.containing.your.DynamicConstant'
    
    constant('myconstant', "I'm an injected constant! YAY!")
}
```
You can specify any value or variable as a constant. You can also specify multiple constants at once using a map:
```groovy
modutil {
    constantAnnotation 'package.containing.your.DynamicConstant'
    
    constants myconstant: "I'm an injected constant! YAY!", myotherconstant: "I'm yet another constant!"
}
```
Lastly, you can also use a closure to obtain the constant at execution time. The closure is triggered for each constant to be injected, given the name of the constant. It must get the value for the constant given by name, or return null if that constant can't be resolved. You may choose to handle any value given here and not returning null at all.
```groovy
modutil {
    constantAnnotation 'package.containing.your.DynamicConstant'
    
    constants { name ->
        if(name == 'myconstant')
            return "I'm an injected constant! YAY!"
        return null
    }
}
```

### Package shading
Package shading is the process of moving embedded dependencies in your jar file under a different package, so that you won't get conflicts with other mods having the same dependency embedded. Defining a package to be shaded is as simple as specifying it in the `modutil` configuration block:
```groovy
modutil {
    shade('external.embedded.package', 'your.mod.package.shadedpackages.external.embedded.package')
}
```
This will move and refactor all classes in your jar file that are in the package `external.embedded.package` or any subpackage into the package `your.mod.package.shadedpackages.external.embedded.package` or any respective subpackage. The package shader will look for usages in your jar file (only in class files, not sources) and rename them to match the new package location.

Real-world example would be to include the [PTG](https://github.com/ShadewRG/PTG) library into your mod's root package:
```groovy
modutil {
    shade('net.shadew.ptg', 'mymod.net.shadew.ptg')
}
```

### Changelog generation
ModUtil provides tools for generating changelog files in two different formats: Markdown and Forge's version.json. The changelog generator takes a JSON file as input, and generates or updates any generated changelog file according to what's in that file.

To configure the changelog generator, first create a JSON file somewhere in your project. The JSON file has the following format:
```json
{
  "version": {
    "number": "1.0",
    "name": "Nice Version Name",
    "minecraft": "1.16.3",
    "stable": true
  },
  "description": [
    "A description of your version.",
    "All strings here are joined by spaces.",
    "To add a newline, use: \n",
    "Markdown generator adds markdown newlines automatically"
  ],
  "changelog": [
    "Add changelog entries here",
    "Changelog entries don't appear in the version.json",
    "They only appear in the Markdown files",
    "The changelog is rendered as a bullet list",
    "Each changelog entry has a separate bullet in that list"
  ]
}
```

The `version.number` must be a valid version number according to the [Semantic Version 2.0.0 specification](https://semver.org/), except that you may ignore the patch number if it's zero. The Minecraft version may be any string, the changelog generator does not bother about this.

To let ModUtil know about your changelog JSON, specify it in the `modutil` block:
```groovy
modutil {
    changelogJson file("$rootDir/changelog.json")
}
```

Now that you have specified a changelog JSON file, you may specify some output files. The changelog generator can generate and update multiple files, whether they are of the same type or not. To specify a markdown output file, use `markdownChangelog` in your config block:

```groovy
modutil {
    changelogJson file("$rootDir/changelog.json")
    markdownChangelog file("$rootDir/CHANGELOG_LATEST.md")
}
```
The Markdown file is deleted and re-created every time you update your versions. The above JSON configuration generates the following output:
```markdown
## 1.0.0 - Nice Version Name

**For Minecraft 1.16.3**

A description of your version. All strings here are joined by spaces. To add a newline, use:   
Markdown generator adds markdown newlines automatically

#### Changelog

- Add changelog entries here
- Changelog entries don't appear in the version.json
- They only appear in the Markdown files
- The changelog is rendered as a bullet list
- Each changelog entry has a separate bullet in that list
```

You can also generate a version.json for Forge, which can be used in Forge's version checker. To specify a version.json output, use `updateJson` in your config block:

```groovy
modutil {
    changelogJson file("$rootDir/changelog.json")
    markdownChangelog file("$rootDir/CHANGELOG_LATEST.md")
    updateJson file("$rootDir/version.json")
}
```

Unlike markdown files, this version.json file is parsed and updated every time you update your versions, and if parsed successfully, does not get overwritten but instead updated. The version.json will not contain the full changelog, only the description and version. The above changelog JSON generates the following (when no version.json was generated yet):

```json
{
  "1.16.3": {
    "1.0": "1.0 - Nice Version Name: A description of your version. All strings here are joined by spaces. To add a newline, use: \n Markdown generator adds markdown newlines automatically"
  },
  "promos": {
    "1.16.3-latest": "1.0",
    "1.16.3-recommended": "1.0"
  }
}
```

Based on whether you have set the `stable` flag in your changelog JSON it updates the latest and recommended build. If your stable flag is set to true, it will update both recommended and latest versions to your new version. If it's set to false it will only update the latest version. When the version.json file already exists, it will not touch the versions already in there, it will only add new ones and update latest and recommended builds.

Now that your changelog generator is configured, you might want to acutally generate the files. There are three tasks for that:
- `genChangelogMarkdown` generates the Markdown output files you have configured
- `updateVersionJson` will update the version.json files you have configured
- `makeVersionInfo` will do both of the above tasks
