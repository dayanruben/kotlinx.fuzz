# Bufer::indexOf and Source::indexOf inconsistency

## Code

```kotlin
val buf = Buffer()
val bs = ByteString(0, 0)
val idx = -1L
assertEquals(-1, buf.indexOf(bs, idx))
assertEquals(-1, (buf as Source).indexOf(bs, idx)) // throws IllegalArgumentException
```

## Expected behavior

`Buffer::indexOf` and `Source::indexOf` works the same

## Actual behavior

Under some conditions, `Buffer::indexOf` retuns `-1` while `Source::indexOf` throws
`IllegalArgumentException`

# Buffer::indexOf and Buffer::copy work strangely together

## Code

```kotlin
val origBuf = Buffer()
origBuf.writeByte(0)
val copyBuf = origBuf.copy()

assertThrows<Throwable> { origBuf.write(Buffer().asInputStream(), 1) }
assertThrows<Throwable> { copyBuf.write(Buffer().asInputStream(), 1) }

val bs = ByteString(0, 0)
val idx = -1L
assertEquals(-1, origBuf.indexOf(bs, idx))
assertEquals(-1, copyBuf.indexOf(bs, idx)) // fails, indexOf == 0
```

## Expected behavior

Buffer copy works the same (at least on methods used in snippet)

## Actual behavior

Under some conditions, `indexOf` on original returns `-1`, while on copy -- `0`
