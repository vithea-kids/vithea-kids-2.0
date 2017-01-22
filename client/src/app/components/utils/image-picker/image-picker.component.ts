import { Component, Input, Provider, forwardRef} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { Resource } from '../../../models/resource';
import { ResourcesService } from '../../../services/resources/resources.service';

export const CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR: any = {
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ImagePickerComponent),
    multi: true
};

@Component({
  selector: 'image-picker',
  templateUrl: './image-picker.component.html',
  styleUrls: ['./image-picker.component.css'],
  providers: [CUSTOM_INPUT_CONTROL_VALUE_ACCESSOR]
})
export class ImagePickerComponent implements ControlValueAccessor {

  @Input() multiSelect : boolean;

  private _items: Array<Resource> = [];

  constructor(public resourcesService: ResourcesService) { }

  get items(): Array<Resource> {
    if (!this._items) this._items = [];

    if (this._items.length === 0 && this.resourcesService.resources) {
      //value copy
      this.resourcesService.resources.forEach((x) => {
        this._items.push(Object.assign({}, x));
      });
    }
    return this._items;
  }

  set items(i: Array<Resource>) {
    if (i !== this._items) {
      this._items = i;
      this.onChange(i);
    }
  }

  writeValue(i: Array<Resource>) {
    this._items = i;
    this.onChange(i);
  }

  toggleItem(item) {
    item.selected = !item.selected;
    if(!this.multiSelect) {
      this._items.forEach((x) => {
        if(x !== item) {
          x.selected = false;
        }
      });
    }
  }

  onChange = (_) => { };
  onTouched = () => { };
  registerOnChange(fn: (_: any) => void): void { this.onChange = fn; }
  registerOnTouched(fn: () => void): void { this.onTouched = fn; }

}