# CMakeLists.txt for Gumbo library
# This file assumes Gumbo's source files are present in its 'src' subdirectory.

add_library(gumbo_static STATIC
    src/attribute.c
    src/char_ref.c
    src/error.c
    src/parser.c
    src/string_buffer.c
    src/string_piece.c
    src/tag.c
    src/tokenizer.c
    src/utf8.c
    src/vector.c
    src/util.c # Added util.c for missing symbols
)

target_include_directories(gumbo_static PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR} # For gumbo.h and other headers in the root
    ${CMAKE_CURRENT_SOURCE_DIR}/src # For internal headers in src/
)
