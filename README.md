# IdGenerator

This package defines a class that supplies unique integer identifiers. It keeps track of which IDs are
free, so that each time it is asked to allocate an ID, the returned value is an ID which was not previously
allocated. IDs remain allocated until they are explicitly freed.

In general, an ID that has been allocated, and subsequently freed, will not be reallocated until all the IDs in
the range supported by the generator have been used. The generator always tries to cycle through all the supported IDs before reallocating any IDs; this is achieved by applying the following rules on each allocation:
* If there are any free IDs that are greater than the last ID allocated, the smallest such ID is allocated.
* Otherwise, the smallest free ID is allocated (effectively starting the cycle from as close as possible
to the start of the range).

## Public interface

### Construction
A new generator is created by specifying the (inclusive) bounds of the range of identifiers to be managed by
the generator. It is not necessary to specify the bounds in any particular order; so `new IdGenerator(0, 100)` and `new IdGenerator(100, 0)` are entirely equivalent. The generator will never generate an ID that is less than the smaller bound, or more than the greater bound.

### Allocation
The `allocateId` method is called to allocate an ID. If all the IDs managed by the generator are allocated, an `IllegalStateException` is thrown; otherwise, an ID is allocated, according to the rules shown above, and returned.

### Deallocation
An ID is freed by passing it to the `free` method. If the ID passed to this method is outside the bounds supported by the allocator, or is already free, the method has no effect; otherwise, the specified ID is freed and becomes eligible for reallocation.

## Implementation details

### Data structures
The allocator maintains a list of those IDs that are free (eligible for allocation), in the form of a sorted set of `Range` objects. The `Range` class is an inner class of the `IdGenerator` class, which represents a range of contiguous numbers by holding the first and last values in that range. The allocator's set of free ranges contains as many `Range` objects as are necessary, which do not overlap and are stored in ascending order of their start values.

The allocator also maintains a `nextId`, which is initially set to the lowest number in the range supported by the allocator, but after each allocation is updated to hold a number one more than the last allocated ID.

### Allocation
The process followed to allocate an ID is:
* If the list of free ranges is empty, throw an `IllegalStateException`.
* Determine the range in the list of free ranges from which the allocation should be made:
    * If the range that immediately precedes the current value of `nextId` (that is, the range with the highest start point that is less than that value) includes `nextId`, select that range.
    * Otherwise, if there is a range that immediately follows the current value of `nextId` (that is, the range with the lowest start point that is not less than that value), select that range.
    * Otherwise, select the first range in the list of free ranges. 
* If the range selected in the previous step contains the value of `nextId`, allocate `nextId`, otherwise allocate the first value in the range.
* Replace the selected range in the free ranges list with up to two ranges: one containing all the IDs in the range that precede the allocated value (if there are any), and one containing all the IDs in the range that follow the allocated value (if there are any).

### Deallocation
The process followed to free (deallocate) an ID is:
* If the specified ID is outside the range supported by the generator, do nothing.
* Find the immediate neighbours, in the list of free ranges, of the ID being freed: that is, the range with the highest start point that is less than that value, and the range with the lowest start point that is not less than that value.
* If either of the immediate neighbour ranges contains the specified ID, do nothing more as it is already free.
* If either of the immediate neighbour ranges adjoins the specified ID (that is, it has an end point that is one less than that value, or a start point that is one more than that value), merge the adjacent ranges together into a new range that includes all numbers in both ranges. If neither does, the new range simply consists of the freed ID.
* Remove either or both of the immediate neighbour ranges from the list of free ranges, if they overlap the range created in the previous step.
* Add the newly-created range to the list of free ranges.