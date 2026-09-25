import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { Distribution } from './models';

@Component({
    selector: 'app-chart',
    imports: [DecimalPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <section class="chart-section" [attr.aria-label]="title()">
            <h3>{{ title() }}</h3>
            <p class="muted chart-description">{{ description() }}</p>
            @if (data().length) {
                <table class="chart-table">
                    <thead class="sr-only">
                        <tr>
                            <th>Category</th>
                            <th>Distribution</th>
                            <th>Instances</th>
                        </tr>
                    </thead>
                    <tbody>
                        @for (item of data(); track item.label) {
                            <tr>
                                <th scope="row">{{ item.label }}</th>
                                <td class="bar-cell">
                                    <div class="bar-track" aria-hidden="true">
                                        <div
                                            class="bar-fill"
                                            [class.bar-unknown]="item.label === 'Not reported'"
                                            [style.width.%]="item.percent"
                                        ></div>
                                    </div>
                                </td>
                                <td class="chart-value">
                                    {{ item.count }} <span class="muted">{{ item.percent | number: '1.0-0' }}%</span>
                                </td>
                            </tr>
                        }
                    </tbody>
                </table>
            } @else {
                <p class="empty-inline">No matching instances.</p>
            }
        </section>
    `,
})
export class ChartComponent {
    title = input.required<string>();
    description = input.required<string>();
    data = input.required<Distribution[]>();
}
