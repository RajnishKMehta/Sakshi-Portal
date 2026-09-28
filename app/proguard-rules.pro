# R8 / ProGuard rules for Sakshi Portal.
#
# Keep rules should only be added for code that is accessed dynamically,
# such as through reflection, JNI, or a serialization framework.
# Avoid broad package-wide rules because they prevent R8 from shrinking,
# optimizing, and obfuscating unused code.
#
# Example of a precise keep rule:
# -keep,allowoptimization,allowobfuscation class rajnishkmehta.sakshi.portal.SomeClass
#
# Add library-specific rules here only when the library's documentation
# requires them and the library does not already provide consumer rules.
