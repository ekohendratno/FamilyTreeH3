import {Component, OnInit} from '@angular/core';
import {MemberService} from '../member.service';
import {ActivatedRoute} from '@angular/router';
import {FamilyMember} from '../../shared/dtos.model';

@Component({
  selector: 'app-view-table',
  templateUrl: './view-table.component.html',
  styleUrls: ['./view-table.component.css']
})
export class ViewTableComponent implements OnInit {
  displayMembers: FamilyMember[] = null;
  // add array of members if you need correct list of members to do CRUD ops
  selectedMemberId: number = null;

  constructor(private memberService: MemberService, private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.route.parent.url.subscribe(parentUrlSegment => {
      const treeId = +parentUrlSegment[0];
      this.memberService.getMembers(treeId).subscribe(familyMembers => {
        this.displayMembers = familyMembers.map(member => this.swapParentsIfNeeded(member, familyMembers));
      });
    });
  }

  getMember(id: number): FamilyMember {
    return this.displayMembers.find(member => member.id === id);
  }

  getMemberFullName(id: number): string {
    const member = this.getMember(id);

    if (!member) {
      return '-';
    }

    return member.firstName + ' ' + member.lastName;
  }

  onMemberSelected(id: number): void {
    this.selectedMemberId = id;
  }

  private swapParentsIfNeeded(member: FamilyMember, familyMembers: FamilyMember[]): FamilyMember {
    const primary = familyMembers.find(fm => fm.id === member.primaryParentId);
    if (!primary) {
      return member;
    }

    const secondary = familyMembers.find(fm => fm.id === member.secondaryParentId);
    if (!secondary) {
      return member;
    }

    if (primary.gender === 'FEMALE' && secondary.gender === 'MALE') {
      member.primaryParentId = secondary.id;
      member.secondaryParentId = primary.id;
      return member;
    }

    return member;
  }
}
