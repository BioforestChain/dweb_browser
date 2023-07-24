export class DragSortableList {
  private container: HTMLElement;
  private items: HTMLElement[];

  constructor(container: HTMLElement) {
    this.container = container;
    this.items = Array.from(container.children) as HTMLElement[];

    this.init();
  }

  private init() {
    this.items.forEach((item, index) => {
      item.addEventListener("dragstart", (event) => {
        event.dataTransfer!.setData("text/plain", index.toString());
        item.style.position = "absolute";
        item.style.transition = "all 0.2s ease-in-out";
      });
      item.addEventListener("dragover", (event) => {
        event.preventDefault();
        const draggedIndex = parseInt(event.dataTransfer!.getData("text/plain"));
        const targetIndex = this.getIndex(event.clientY);
        if (draggedIndex !== targetIndex) {
          this.moveItem(draggedIndex, targetIndex);
        }
      });
    });
    this.container.addEventListener("drop", (event) => {
      event.preventDefault();
      const draggedIndex = parseInt(event.dataTransfer!.getData("text/plain"));
      const targetIndex = this.getIndex(event.clientY);
      if (draggedIndex !== targetIndex) {
        this.moveItem(draggedIndex, targetIndex);
      }
      this.items[targetIndex].style.position = "absolute";
      this.items[targetIndex].style.transform = `translateY(${targetIndex * 110}px)`;
    });
  }

  private getIndex(y: number): number {
    return this.items.reduce(
      (closestIndex, item, index) => {
        const rect = item.getBoundingClientRect();
        const offset = y - rect.top - rect.height / 2;
        if (offset < 0 && offset > closestIndex.distance) {
          return { distance: offset, index };
        } else {
          return closestIndex;
        }
      },
      { distance: Number.NEGATIVE_INFINITY, index: -1 }
    ).index;
  }

  private moveItem(fromIndex: number, toIndex: number) {
    const item = this.items.splice(fromIndex, 1)[0];
    this.items.splice(toIndex, 0, item);
    this.container.insertBefore(item, this.items[toIndex + 1]);
    this.items.forEach((item, index) => {
      item.style.transform = `translateY(${index * 110}px)`;
    });
  }
}
